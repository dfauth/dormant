package io.github.dfauth.dormant.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.dfauth.dormant.model.utils.LocalDateDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.function.Predicate.not;

@Slf4j
public class MarketHoursIoClient {

    private final String apiKey;
    private final WebClient.Builder webClientBuilder;

    public MarketHoursIoClient(String apiKey) {
        this.apiKey = apiKey;
        this.webClientBuilder = WebClient.builder()
                .baseUrl("https://markethours.io")
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create().wiretap(true) // Enables payload tracking
                ));
        Optional.ofNullable(this.apiKey)
                .ifPresent(a -> this.webClientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + a));
    }

    public Mono<Collection<LocalDate>> getMarketHolidays(String code, LocalDate start, LocalDate end) {
        // /api/markets/holidays?markets=nyse,nasdaq
        return webClientBuilder
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder.path("/api/markets/holidays")
                        .queryParam("markets", code)
                        .build())
                .retrieve()
                .onStatus(not(HttpStatusCode::is2xxSuccessful), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    // Log or inspect the raw error body content
                                    System.err.println("Error Body: " + errorBody);
                                    return Mono.error(new RuntimeException("API error: " + errorBody));
                                })
                )
                .bodyToMono(new ParameterizedTypeReference<Response>() {})
                .doOnError(ex -> log.error(ex.getMessage(), ex))
                .map(r -> r.data().getFirst().holidays().stream().filter(PayloadElement::isFullClose).map(PayloadElement::date).toList());
    }

    /**

     {
     "success": true,
     "data": [
     {
     "id": "nyse",
     "name": "New York Stock Exchange",
     "shortName": "NYSE",
     "country": "United States",
     "holidays": [
     { "date": "2026-01-01", "name": "New Year's Day", "type": "full" },
     { "date": "2026-01-19", "name": "Martin Luther King Jr. Day", "type": "full" }
     ]
     }
     ]
     }     */
    private record Response(boolean success, List<Data> data) {}
    private record Data(String id, String name, String shortName, String country, List<PayloadElement> holidays) {}

    // date=2026-12-25, status=full_close, close_time=null, name=Christmas Day
    record PayloadElement(@JsonDeserialize(using = LocalDateDeserializer.class) LocalDate date, HolidayStatus status, String name) {
        public boolean isFullClose() {
            return status.isFullClose();
        }
    }

    enum HolidayStatus {
        full_close, early_close;

        public boolean isFullClose() {
            return this == full_close;
        }
    }
}
