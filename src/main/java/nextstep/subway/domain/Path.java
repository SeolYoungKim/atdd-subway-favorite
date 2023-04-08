package nextstep.subway.domain;

import java.util.List;

public class Path {
    private Sections sections;

    public Path(Sections sections) {
        if (sections.getStations().isEmpty()) {
            throw new IllegalArgumentException("서로 연결되지 않은 역입니다.");
        }
        this.sections = sections;
    }

    public Sections getSections() {
        return sections;
    }

    public int extractDistance() {
        return sections.totalDistance();
    }

    public List<Station> getStations() {
        return sections.getStations();
    }
}
