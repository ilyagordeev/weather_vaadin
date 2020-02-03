package xyz.mathic.weather.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import xyz.mathic.weather.model.Weather;
import xyz.mathic.weather.model.WeatherBuilder;
import xyz.mathic.weather.model.WeatherRepository;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WeatherUpdater {
    @Value("${token.google}")
    private String TokenGoogleApi;
    @Value("${token.yandex}")
    private String TokenYandexApi;
    @Value("${token.openweather}")
    private String TokenOpenWeatherApi;

    private final RestTemplate rest;
    private final WeatherRepository weatherRepository;

    public WeatherUpdater(WeatherRepository weatherRepository) {
        this.weatherRepository = weatherRepository;
        this.rest = new RestTemplateBuilder().build();
    }

    /**
     * Публичный метод, производит все необходимые проверки входящих параметров, решает нужно ли обновлять погоду
     * @param location адрес
     * @param weatherProvider провайдер погоды
     * @return Pojo Weather
     */
    public Weather getWeather(String location, String weatherProvider) {
        if (location == null || location.isEmpty() || weatherProvider == null || weatherProvider.isEmpty()) return null;
        boolean updated = true; // update status, true if success

        Weather weather = weatherRepository.findFirstByCityAndWeatherProvider(location, weatherProvider).isPresent() ?
                weatherRepository.findFirstByCityAndWeatherProvider(location, weatherProvider).get() : null;
        if (weather == null) {
            if (!citiesInUpdate.contains(location)) updated = Update(location, weatherProvider);
            else return null;
        } else if (weather.expired()) updated = Update(location, weatherProvider);
            else return  weather;
        if (updated) return weatherRepository.findFirstByCityAndWeatherProvider(location, weatherProvider).get();
        return null;
    }

    /**
     * Агрегация разных api погоды
     * @param city адрес
     * @param weatherProvider провайдер погоды
     * @return успешность обновления погоды
     */
    private boolean Update(String city, String weatherProvider) {
        if (weatherProvider.equals("Yandex")) return RequestYandex(city);
        if (weatherProvider.equals("OpenWeather")) return RequestOpenWeather(city);
        return false;
    }

    /**
     * Сет адресов, для которых осуществляется запрос координат
     */
    private static Set<String> citiesInUpdate = new HashSet<>();

    /**
     * Кэш геоданных
     */
    private static Map<String, Pair<String, String>> coordinatesCache = new ConcurrentHashMap<String, Pair<String, String>>()
    {{
        put("екатеринбург", Pair.of("lat=56.836331&lon=60.605546", "Россия, Свердловская область"));
        put("москва", Pair.of("lat=55.748952&lon=37.620076", "Россия"));
        put("нью-йорк", Pair.of("lat=40.726063&lon=-73.822881", "США"));
        put("амстердам", Pair.of("lat=52.377109&lon=4.897162", "Нидерланды"));
        put("магадан", Pair.of("lat=59.563922&lon=150.814959", "Россия, Магаданская область"));
    }};

    /**
     * Метод работы с кэшем геоданных
     * @param city адрес
     * @return пара: адрес - координаты
     */
    private Pair<String, String> Coordinates(String city) {
        if (!coordinatesCache.containsKey(city)) {
            Pair<String, String> coordinates = Geocode(city);
            if (coordinates != null) {
                coordinatesCache.put(city, coordinates);
                return coordinates;
            }
        } else {
            return coordinatesCache.get(city);
        }
        return null;
    }

    /**
     * Вынесенный метод запроса к api геоданных и погоды
     * @param url адрес api
     * @param header true если запрос к api Яндекса
     * @return тело ответа
     */
    private String Request(String url, boolean header) {
        String body = "";
        HttpHeaders headers = new HttpHeaders();

        if (header)
            headers.add("X-Yandex-API-Key", TokenYandexApi);

        HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> responseEntity = rest.exchange(url, HttpMethod.GET, requestEntity, String.class);
        return responseEntity.getBody();
    }

    /**
     * Метод запроса к серверу геоданных
     * @param city любой адрес, локация
     * @return пара: локация - координаты
     */
    private Pair<String, String> Geocode(String city) {
        citiesInUpdate.add(city.toLowerCase());
        String url = String.format("https://maps.googleapis.com/maps/api/geocode/json?address=%s&key=" + TokenGoogleApi,
                city);

        String response = Request(url, false);

        ObjectMapper objectMapper = new ObjectMapper();
        assert response != null;

        try {
            JsonNode rootNode = objectMapper.readTree(response);
            String status = rootNode.path("status").asText();
            if (status.equals("ZERO_RESULTS")) return null;
            String lat = rootNode.path("results").get(0).path("geometry").path("location").path("lat").asText();
            String lng = rootNode.path("results").get(0).path("geometry").path("location").path("lng").asText();
            String address = rootNode.path("results").get(0).path("formatted_address").asText();
            System.out.println(address);
            citiesInUpdate.remove(city.toLowerCase());
            if (status.equals("OK")) return Pair.of("lat=" + lat + "&lon=" + lng, address);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Метод запроса погоды к серверу Yandex
     * @param city любой адрес, локация
     * @return успешность выполнения запроса
     */
    private boolean RequestYandex(String city) {
        Pair<String, String> coordinates = Coordinates(city.toLowerCase());
        if (coordinates == null) return false;
        String url = String.format("https://api.weather.yandex.ru/v1/informers?%s",
                coordinates.getFirst());

        String response = Request(url, true);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            assert response != null;
            JsonNode rootNode = objectMapper.readTree(response);
            String temp = rootNode.path("fact").path("temp").asText();
            String wind = rootNode.path("fact").path("wind_speed").asText();
            String pressure = rootNode.path("fact").path("pressure_pa").asText();
            String humidity = rootNode.path("fact").path("humidity").asText();

            Weather weather = new WeatherBuilder()
                    .withHumidity(humidity)
                    .withPressure(pressure)
                    .withTemp(temp)
                    .withWind(wind)
                    .withCity(city)
                    .withAddress(coordinates.getSecond())
                    .withWeatherProvider("Yandex").build();

            if (weatherRepository.findFirstByCityAndWeatherProvider(city, "Yandex").isPresent())
                weatherRepository.delete(weatherRepository.findFirstByCityAndWeatherProvider(city, "Yandex").get());

            weatherRepository.save(weather);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Метод запроса погоды к серверу OpenWeather
     * @param city любой адрес, локация
     * @return успешность выполнения запроса
     */
    private boolean RequestOpenWeather(String city) {
        Pair<String, String> coordinates = Coordinates(city.toLowerCase());
        if (coordinates == null) return false;
        String url = String.format("https://api.openweathermap.org/data/2.5/weather?%s&units=metric&APPID=" + TokenOpenWeatherApi,
                coordinates.getFirst());

        String response = Request(url, false);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            assert response != null;
            JsonNode rootNode = objectMapper.readTree(response);
            String temp = rootNode.path("main").path("temp").asText();
            String wind = rootNode.path("wind").path("speed").asText();
            String pressure = rootNode.path("main").path("pressure").asText();
            String humidity = rootNode.path("main").path("humidity").asText();

            Weather weather = new WeatherBuilder()
                    .withHumidity(humidity)
                    .withPressure(pressure)
                    .withTemp(temp)
                    .withWind(wind)
                    .withCity(city)
                    .withAddress(coordinates.getSecond())
                    .withWeatherProvider("OpenWeather").build();

            if (weatherRepository.findFirstByCityAndWeatherProvider(city, "OpenWeather").isPresent())
                weatherRepository.delete(weatherRepository.findFirstByCityAndWeatherProvider(city, "OpenWeather").get());

            weatherRepository.save(weather);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;

    }
}
