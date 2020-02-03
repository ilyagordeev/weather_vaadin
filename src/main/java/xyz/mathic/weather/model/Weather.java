package xyz.mathic.weather.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;

/**
 * Pojo погоды
 */
@Entity
@Data
public class Weather {
    @Id
    @GeneratedValue
    private Long id;
    private Long UpdateTime = 500000L;
    private String city;
    private String weatherProvider;
    private String temp;
    private String wind;
    private String pressure;
    private String humidity;
    private String address;
    private Long requestTime;

    public Weather() {
        this.requestTime = new Date().getTime();
    }
    public boolean expired() {
        return (new Date().getTime() - requestTime) >= UpdateTime;
    }
}
