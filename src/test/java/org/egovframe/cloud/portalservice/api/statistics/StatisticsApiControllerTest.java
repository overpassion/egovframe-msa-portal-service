package org.egovframe.cloud.portalservice.api.statistics;

import org.egovframe.cloud.portalservice.api.policy.dto.PolicyUpdateRequestDto;
import org.egovframe.cloud.portalservice.api.statistics.dto.StatisticsResponseDto;
import org.egovframe.cloud.portalservice.api.statistics.dto.StatisticsYMRequestDto;
import org.egovframe.cloud.portalservice.domain.statistics.Statistics;
import org.egovframe.cloud.portalservice.domain.statistics.StatisticsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class StatisticsApiControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StatisticsRepository statisticsRepository;


    @BeforeEach
    public void setup() {
        for (int i = 0; i < 10; i++) {
            statisticsRepository.save(Statistics.builder()
                    .siteId(1L)
                    .remoteIp("testip")
                    .build());
        }

    }

    @AfterEach
    public void tearDown() {
        statisticsRepository.deleteAll();
    }

    @Test
    public void 월별접속통계_조회_성공() throws Exception {
        Long siteId = 1L;
        // when
        ResponseEntity< List<StatisticsResponseDto>> responseEntity =
                restTemplate.exchange("/api/v1/statistics/monthly/"+siteId,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<StatisticsResponseDto>>(){});

        responseEntity.getBody().forEach(System.out::println);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody().size()).isEqualTo(1);
        assertThat(responseEntity.getBody().get(0).getY()).isEqualTo(10);
    }

    @Test
    public void 일별접속통계_조회_성공() throws Exception {
        Long siteId = 1L;

        // when
        ResponseEntity< List<StatisticsResponseDto>> responseEntity =
                restTemplate.exchange("/api/v1/statistics/daily/"+siteId+"?year=2021&month=9",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<StatisticsResponseDto>>(){});

        responseEntity.getBody().forEach(System.out::println);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody().size()).isEqualTo(1);
        assertThat(responseEntity.getBody().get(0).getY()).isEqualTo(10);
    }


}