package xyz.mathic.weather.view;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.PWA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.mathic.weather.controller.WeatherUpdater;
import xyz.mathic.weather.model.Weather;
import xyz.mathic.weather.model.WeatherRepository;

@PWA(name = "Simple Weather", shortName = "SW")
@Route
public class MainView extends VerticalLayout{

    private static final Logger logger = LoggerFactory.getLogger(MainView.class);
    final WeatherRepository weatherRepository;
    final WeatherUpdater weatherUpdater;
    final TextField cityFilter;
    final RadioButtonGroup<String> group;
    final Label location;
    final Div temp, wind, pressure, humidity;
    final Button city1, city2, city3, city4, city5;

    public MainView(WeatherRepository weatherRepository, WeatherUpdater weatherUpdater) {
        this.weatherRepository = weatherRepository;
        this.weatherUpdater = weatherUpdater;

        this.location = new Label();
        this.group = new RadioButtonGroup<>();
        this.cityFilter = new TextField();

        this.temp = new Div();
        this.wind = new Div();
        this.pressure = new Div();
        this.humidity = new Div();

        this.city1 = new Button("Екатеринбург");
        this.city2 = new Button("Москва");
        this.city3 = new Button("Нью-Йорк");
        this.city4 = new Button("Амстердам");
        this.city5 = new Button("Магадан");

        setPadding(true);
        setMargin(true);
        setSpacing(true);

        cityFilter.setPlaceholder("Location");
        cityFilter.setWidth("550px");
        Icon icon = VaadinIcon.SEARCH.create();
        cityFilter.setPrefixComponent(icon);
        Button sendButton = new Button("Search", event -> updateCity());
        HorizontalLayout searchContainer = new HorizontalLayout(cityFilter, sendButton);
        sendButton.addClickShortcut(Key.ENTER).listenOn(searchContainer);


        group.setItems("OpenWeather", "Yandex");
        group.setValue("OpenWeather");
        group.addValueChangeListener(event -> {if (!cityFilter.isEmpty()) updateCity();});

        H1 heading = new H1("Simple Weather");

        temp.setWidth("250px");
        wind.setWidth("250px");
        pressure.setWidth("250px");
        humidity.setWidth("250px");
        HorizontalLayout weatherContainer = new HorizontalLayout(temp, wind, pressure, humidity);

        city1.addClickListener(event -> {cityFilter.setValue(city1.getText()); updateCity();});
        city2.addClickListener(event -> {cityFilter.setValue(city2.getText()); updateCity();});
        city3.addClickListener(event -> {cityFilter.setValue(city3.getText()); updateCity();});
        city4.addClickListener(event -> {cityFilter.setValue(city4.getText()); updateCity();});
        city5.addClickListener(event -> {cityFilter.setValue(city5.getText()); updateCity();});
        city1.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        city2.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        city3.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        city4.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        city5.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        HorizontalLayout linksContainer = new HorizontalLayout(city1, city2, city3, city4, city5);

        add(heading, linksContainer, location, searchContainer, group, weatherContainer);

        cityFilter.setValue("Екатеринбург");
        updateCity();
    }

    void updateCity() {
        Notification notification = new Notification(
                "This location not found", 3000,
                Notification.Position.TOP_CENTER);

        String city = cityFilter.getValue();
        String prov = group.getValue();
        logger.info(city);
        Weather weather = weatherUpdater.getWeather(city, prov);
        if (weather == null) {
            notification.open();
        } else {
            location.setText(weather.getAddress());
            temp.removeAll();
            temp.setText("Температура");
            temp.add(new H1(weather.getTemp() + "°C"));
            wind.removeAll();
            wind.setText("Скорость ветра");
            wind.add(new H1(weather.getWind() + " м/с"));
            pressure.removeAll();
            pressure.setText("Давление");
            pressure.add(new H1(weather.getPressure() + " гПа"));
            humidity.removeAll();
            humidity.setText("Влажность воздуха");
            humidity.add(new H1(weather.getHumidity() + "%"));
        }
    }

}
