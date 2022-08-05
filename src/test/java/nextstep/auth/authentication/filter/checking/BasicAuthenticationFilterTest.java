package nextstep.auth.authentication.filter.checking;

import nextstep.auth.authentication.AuthenticationToken;
import nextstep.member.application.LoginMemberService;
import nextstep.member.domain.LoginMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BasicAuthenticationFilterTest {

    private static final String EMAIL = "email@email.com";
    private static final String PASSWORD = "password";

    private BasicAuthenticationFilter filter;
    private LoginMemberService loginMemberService;

    @BeforeEach
    void setUp() {
        loginMemberService = mock(LoginMemberService.class);
        filter = new BasicAuthenticationFilter(loginMemberService);
    }

    @Test
    void convert() throws IOException {
        var authenticationToken = filter.convert(createMockRequest());

        assertAll(
                () -> assertThat(authenticationToken.getPrincipal()).isEqualTo(EMAIL),
                () -> assertThat(authenticationToken.getCredentials()).isEqualTo(PASSWORD)
        );
    }

    @Test
    void authenticate() {
        when(loginMemberService.loadUserByUsername(EMAIL))
                .thenReturn(new LoginMember(EMAIL, PASSWORD, List.of("ROLE_ADMIN")));

        var authentication = filter.authenticate(new AuthenticationToken(EMAIL, PASSWORD));

        assertAll(
                () -> assertThat(authentication.getPrincipal()).isEqualTo(EMAIL),
                () -> assertThat(authentication.getAuthorities()).containsExactly("ROLE_ADMIN")
        );

    }

    private MockHttpServletRequest createMockRequest() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        var encoded = Base64.getEncoder().encodeToString((EMAIL + ":" + PASSWORD).getBytes());
        request.addHeader("authorization", "Basic " + encoded);
        return request;
    }

}