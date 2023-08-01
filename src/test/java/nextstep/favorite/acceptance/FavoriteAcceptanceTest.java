package nextstep.favorite.acceptance;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.acceptance.step.SectionStep;
import nextstep.subway.common.exception.ErrorCode;
import nextstep.utils.AcceptanceTest;
import nextstep.utils.ErrorTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static nextstep.auth.acceptance.step.TokenSteps.일반_로그인_요청;
import static nextstep.auth.acceptance.step.TokenSteps.토큰_추출;
import static nextstep.favorite.acceptance.step.FavoriteSteps.*;
import static nextstep.member.acceptance.step.MemberSteps.회원_생성_요청;
import static nextstep.subway.acceptance.step.LineStep.지하철_노선을_생성한다;
import static nextstep.subway.acceptance.step.StationStep.지하철역을_생성한다;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("즐겨찾기 관련 기능")
public class FavoriteAcceptanceTest extends AcceptanceTest {
    public static final String EMAIL = "email@email.com";
    public static final String PASSWORD = "password";
    public static final int AGE = 20;

    private Long 교대역;
    private Long 강남역;
    private Long 양재역;
    private Long 남부터미널역;
    private Long 이호선;
    private Long 신분당선;
    private Long 삼호선;

    /**
     * 교대역    --- *2호선* ---   강남역
     * |                        |
     * *3호선*                   *신분당선*
     * |                        |
     * 남부터미널역  --- *3호선* ---   양재
     */
    @BeforeEach
    public void setUp() {
        super.setUp();

        교대역 = Id_추출(지하철역을_생성한다("교대역"));
        강남역 = Id_추출(지하철역을_생성한다("강남역"));
        양재역 = Id_추출(지하철역을_생성한다("양재역"));
        남부터미널역 = Id_추출(지하철역을_생성한다("남부터미널역"));

        이호선 = Id_추출(지하철_노선을_생성한다("2호선", "green", 교대역, 강남역, 10));
        신분당선 = Id_추출(지하철_노선을_생성한다("신분당선", "red", 강남역, 양재역, 10));
        삼호선 = Id_추출(지하철_노선을_생성한다("3호선", "orange", 교대역, 남부터미널역, 2));

        SectionStep.지하철_노선_구간을_등록한다(삼호선, 남부터미널역, 양재역, 3);
    }

    private Long Id_추출(ExtractableResponse<Response> response) {
        return response.jsonPath().getLong("id");
    }

    /**
     * ## 시나리오
     * Given : 지하철역과 노선을 생성하고 (@BeforeEach)
     * And : 회원을 생성하고
     * And : 토큰을 발급받은 후
     * When : 출발역 id와 도착역 id를 전송하면
     * Then : 즐겨찾기가 생성(등록)된다.
     */
    @DisplayName("즐겨찾기 생성")
    @Test
    void create() {
        // given
        회원_생성_요청(EMAIL, PASSWORD, AGE);
        String accessToken = 토큰_추출(일반_로그인_요청(EMAIL, PASSWORD));

        // when
        ExtractableResponse<Response> response = 즐겨찾기_생성_요청(TokenType.BEARER, accessToken, 교대역, 양재역);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        ExtractableResponse<Response> responseOfShowFavorites = 즐겨찾기_조회_요청(TokenType.BEARER, accessToken);
        assertThat(즐겨찾기_Id_목록_추출(responseOfShowFavorites)).hasSize(1);
        assertThat(즐겨찾기_시작역_목록_추출(responseOfShowFavorites)).containsExactly(교대역);
        assertThat(즐겨찾기_도착역_목록_추출(responseOfShowFavorites)).containsExactly(양재역);
    }

    /**
     * ## 시나리오
     * Given : 지하철역과 노선을 생성하고 (@BeforeEach)
     * When : 가짜 토큰과 출발역 id와 도착역 id를 전송하면
     * Then : 401 UnAuthorized가 발생한다.
     */
    @DisplayName("즐겨찾기 생성 실패 : 올바른 토큰이 아닌 경우")
    @Test
    void createFailByFakeToken() {
        // given
        String fakeToken = "가짜 토큰";

        // when
        ExtractableResponse<Response> response = 즐겨찾기_생성_요청(TokenType.BEARER, fakeToken, 교대역, 양재역);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        ErrorTestUtils.예외_메세지_검증(response, ErrorCode.INVALID_TOKEN_EXCEPTION);
    }

    /**
     * ## 시나리오
     * Given : 지하철역과 노선을 생성하고 (@BeforeEach)
     * When : 토큰 없이 출발역 id와 도착역 id를 전송하면
     * Then : 401 UnAuthorized가 발생한다.
     */
    @DisplayName("즐겨찾기 생성 실패 : 토큰을 보내지 않는 경우")
    @Test
    void createFailByNoToken() {
        // when
        ExtractableResponse<Response> response = 토큰_헤더_없이_즐겨찾기_생성_요청(교대역, 양재역);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    /**
     * ## 시나리오
     * Given : 지하철역과 노선을 생성하고 (@BeforeEach)
     * And : 회원을 생성하고
     * And : 토큰을 발급받은 후
     * When : Bearer가 아닌 Basic 토큰과 출발역 id와 도착역 id를 전송하면
     * Then : 401 UnAuthorized가 발생한다.
     */
    @DisplayName("즐겨찾기 생성 실패 : Basic 토큰을 보내는 경우")
    @Test
    void createFailByNoBearer() {
        // given
        회원_생성_요청(EMAIL, PASSWORD, AGE);
        String accessToken = 토큰_추출(일반_로그인_요청(EMAIL, PASSWORD));

        // when
        ExtractableResponse<Response> response = 즐겨찾기_생성_요청(TokenType.BASIC, accessToken, 교대역, 양재역);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    /**
     * Given : 지하철역과 노선을 생성하고 (@BeforeEach)
     * And : 회원을 생성하고
     * And : 토큰을 발급받은 후
     * When : 서로 같은 출발역 id와 도착역 id를 전송하면
     * Then : 400 BadRequest가 발생한다.
     */
    @DisplayName("즐겨찾기 생성 실패 : 역 id가 서로 같은 경우")
    @Test
    void createFailBySameId() {
        // given
        회원_생성_요청(EMAIL, PASSWORD, AGE);
        String accessToken = 토큰_추출(일반_로그인_요청(EMAIL, PASSWORD));

        // when
        ExtractableResponse<Response> response = 즐겨찾기_생성_요청(TokenType.BEARER, accessToken, 교대역, 교대역);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        ErrorTestUtils.예외_메세지_검증(response, ErrorCode.SAME_SOURCE_AND_TARGET_STATION);
    }

    /**
     * Given : 지하철역과 노선을 생성하고 (@BeforeEach)
     * And : 회원을 생성하고
     * And : 토큰을 발급받은 후
     * When : 저장되지 않은 출발역 id와 도착역 id를 전송하면
     * Then : 404 NotFound가 발생한다.
     */
    @DisplayName("즐겨찾기 생성 실패 : 역 id에 해당하는 Station이 없는 경우")
    @Test
    void createFailByStationNotFound() {
        // given
        Long 없는역 = -1L;

        회원_생성_요청(EMAIL, PASSWORD, AGE);
        String accessToken = 토큰_추출(일반_로그인_요청(EMAIL, PASSWORD));

        // when
        ExtractableResponse<Response> response = 즐겨찾기_생성_요청(TokenType.BEARER, accessToken, 교대역, 없는역);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        ErrorTestUtils.예외_메세지_검증(response, ErrorCode.STATION_NOT_FOUND);
    }

    /**
     * Given : 지하철역과 노선을 생성하고 (@BeforeEach)
     * And : 경로가 연결되지 않은 지하철 노선을 생성하고
     * And : 회원을 생성하고
     * And : 토큰을 발급받은 후
     * When : 경로가 없는 출발역 id 또는 도착역 id를 전송하면
     * Then : 404 NotFound가 발생한다.
     */
    @DisplayName("즐겨찾기 생성 실패 : 경로가 없는 경우")
    @Test
    void createFailByStationPathNotFound() {
        // given
        Long 증미역 = Id_추출(지하철역을_생성한다("증미역"));
        Long 등촌역 = Id_추출(지하철역을_생성한다("등촌역"));
        Id_추출(지하철_노선을_생성한다("9호선", "brown", 증미역, 등촌역, 10));

        회원_생성_요청(EMAIL, PASSWORD, AGE);
        String accessToken = 토큰_추출(일반_로그인_요청(EMAIL, PASSWORD));

        // when
        ExtractableResponse<Response> response = 즐겨찾기_생성_요청(TokenType.BEARER, accessToken, 교대역, 등촌역);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        ErrorTestUtils.예외_메세지_검증(response, ErrorCode.PATH_NOT_FOUND);
    }

    /**
     * Given : 지하철역과 노선을 생성하고 (@BeforeEach)
     * And : 주어진 노선 상에 없는 역을 생성하고
     * And : 회원을 생성하고
     * And : 토큰을 발급받은 후
     * When : 주어진 노선 상에 없는 출발역 id 또는 도착역 id를 전송하면
     * Then : 400 BadRequest가 발생한다.
     */
    @DisplayName("즐겨찾기 생성 실패 : 역이 주어진 노선 상에 없는 역인 경우")
    @Test
    void createFailByStationNotInGivenLine() {
        // given
        Long 노선에_없는역 = Id_추출(지하철역을_생성한다("노선에 없는역"));

        회원_생성_요청(EMAIL, PASSWORD, AGE);
        String accessToken = 토큰_추출(일반_로그인_요청(EMAIL, PASSWORD));

        // when
        ExtractableResponse<Response> response = 즐겨찾기_생성_요청(TokenType.BEARER, accessToken, 교대역, 노선에_없는역);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        ErrorTestUtils.예외_메세지_검증(response, ErrorCode.STATION_NOT_IN_GIVEN_LINES);
    }

    /**
     * ## 시나리오
     * Given : 지하철역과 노선을 생성하고 (@BeforeEach)
     * And : 회원을 생성하고
     * And : 토큰을 발급받고
     * And : 즐겨찾기를 등록한 후
     * When : 즐겨찾기 목록 조회를 요청하면
     * Then : 즐겨찾기 목록이 조회된다.
     */
    @DisplayName("즐겨찾기 목록 조회")
    @Test
    void show() {
        // given
        회원_생성_요청(EMAIL, PASSWORD, AGE);
        String accessToken = 토큰_추출(일반_로그인_요청(EMAIL, PASSWORD));

        즐겨찾기_생성_요청(TokenType.BEARER, accessToken, 교대역, 양재역);

        // when
        ExtractableResponse<Response> response = 즐겨찾기_조회_요청(TokenType.BEARER, accessToken);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(즐겨찾기_Id_목록_추출(response)).hasSize(1);
        assertThat(즐겨찾기_시작역_목록_추출(response)).containsExactly(교대역);
        assertThat(즐겨찾기_도착역_목록_추출(response)).containsExactly(양재역);
    }

    private List<Long> 즐겨찾기_Id_목록_추출(ExtractableResponse<Response> response) {
        return response.jsonPath().getList("id", Long.class);
    }

    private List<Long> 즐겨찾기_시작역_목록_추출(ExtractableResponse<Response> response) {
        return response.jsonPath().getList("source.id", Long.class);
    }

    private List<Long> 즐겨찾기_도착역_목록_추출(ExtractableResponse<Response> response) {
        return response.jsonPath().getList("target.id", Long.class);
    }

    /**
     * ## 시나리오
     * Given : 지하철역과 노선을 생성하고 (@BeforeEach)
     * And : 회원을 생성하고
     * And : 토큰을 발급받고
     * And : 즐겨찾기를 2개 등록한 후
     * When : 즐겨찾기 삭제를 요청하면
     * Then : 즐겨찾기가 삭제된다.
     */
    @DisplayName("즐겨찾기 삭제")
    @Test
    void delete() {
        // given
        회원_생성_요청(EMAIL, PASSWORD, AGE);
        String accessToken = 토큰_추출(일반_로그인_요청(EMAIL, PASSWORD));

        Long targetId = Location에서_즐겨찾기_Id_추출(즐겨찾기_생성_요청(TokenType.BEARER, accessToken, 교대역, 양재역));
        Long nonTargetId = Location에서_즐겨찾기_Id_추출(즐겨찾기_생성_요청(TokenType.BEARER, accessToken, 남부터미널역, 양재역));

        // when
        ExtractableResponse<Response> response = 즐겨찾기_삭제_요청(TokenType.BEARER, accessToken, targetId);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());

        ExtractableResponse<Response> responseOfShowFavorites = 즐겨찾기_조회_요청(TokenType.BEARER, accessToken);

        List<Long> 즐겨찾기_Id_목록 = 즐겨찾기_Id_목록_추출(responseOfShowFavorites);
        assertThat(즐겨찾기_Id_목록).hasSize(1);
        assertThat(즐겨찾기_Id_목록).containsExactly(nonTargetId);
    }

    private Long Location에서_즐겨찾기_Id_추출(ExtractableResponse<Response> response) {
        String[] locations = response.header("location").split("/favorites/");
        String favoriteId = locations[1];
        return Long.parseLong(favoriteId);
    }
}
