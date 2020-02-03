package xyz.mathic.weather.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WeatherRepository extends JpaRepository<Weather, Long> {
    Optional<Weather> findFirstByCityAndWeatherProvider (String city, String weatherProvider);
}
