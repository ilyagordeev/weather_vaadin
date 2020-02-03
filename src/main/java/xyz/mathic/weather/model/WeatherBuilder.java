package xyz.mathic.weather.model;

public final class WeatherBuilder {
    private String city;
    private String weatherProvider;
    private String temp;
    private String wind;
    private String pressure;
    private String humidity;
    private String address;

    public WeatherBuilder() {
    }

    public static WeatherBuilder aWeather() {
        return new WeatherBuilder();
    }

    public WeatherBuilder withCity(String city) {
        this.city = city;
        return this;
    }

    public WeatherBuilder withWeatherProvider(String weatherProvider) {
        this.weatherProvider = weatherProvider;
        return this;
    }

    public WeatherBuilder withTemp(String temp) {
        this.temp = temp;
        return this;
    }

    public WeatherBuilder withWind(String wind) {
        this.wind = wind;
        return this;
    }

    public WeatherBuilder withPressure(String pressure) {
        this.pressure = pressure;
        return this;
    }

    public WeatherBuilder withHumidity(String humidity) {
        this.humidity = humidity;
        return this;
    }

    public WeatherBuilder withAddress(String address) {
        this.address = address;
        return this;
    }

    public Weather build() {
        Weather weather = new Weather();
        weather.setCity(city);
        weather.setWeatherProvider(weatherProvider);
        weather.setTemp(temp);
        weather.setWind(wind);
        weather.setPressure(pressure);
        weather.setHumidity(humidity);
        weather.setAddress(address);
        return weather;
    }
}
