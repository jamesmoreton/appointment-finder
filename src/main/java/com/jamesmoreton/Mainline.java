package com.jamesmoreton;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mainline {

  private static final Logger logger = LoggerFactory.getLogger(Mainline.class);

  // Update me
  private static final String TWILIO_USERNAME = "username";
  private static final String TWILIO_PASSWORD = "password";
  private static final String ALERT_NUMBER_TO = "07123456789";
  private static final String ALERT_NUMBER_FROM = "07123456789";

  private static final int SCHEDULE_PERIOD_SECONDS = 5;
  private static final HashMap<AppointmentService, Instant> LAST_NOTIFIED_MAP = new HashMap<>();
  private static final Duration MAX_NOTIFICATION_RATE = Duration.ofSeconds(60);
  private static final HttpClient CLIENT = HttpClient.newBuilder().build();

  public static void main(String[] args) {
    logger.info("Appointment finder starting up...");
    Twilio.init(TWILIO_USERNAME, TWILIO_PASSWORD);
    logger.info("Initialised twilio");

    ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);
    EnumSet.allOf(AppointmentService.class).forEach(service -> {
      logger.info("Initialising appointment service {} scheduled every {} seconds", service,
          SCHEDULE_PERIOD_SECONDS);
      executorService.scheduleAtFixedRate(
          () -> search(service),
          0,
          SCHEDULE_PERIOD_SECONDS,
          TimeUnit.SECONDS
      );
    });
  }

  private static void search(AppointmentService service) {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(service.getUrl()))
        .GET()
        .build();

    HttpResponse<String> response;
    try {
      response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      logger.error("Failed to call {}", service.getDescription(), e);
      return;
    }

    if (response.statusCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
      logger.error("Received a bad response from {}, status code [{}], body [{}]",
          service.getDescription(), response.statusCode(), response.body());
      return;
    }

    if (response.body() == null || response.body().contains(service.getNoAppointmentsText())) {
      logger.info("No appointments found for {} - status [{}]", service.getDescription(),
          response.statusCode());
      return;
    }

    logger.info("Appointments found for {}!!! Status [{}]", service.getDescription(),
        response.statusCode());
    if (hasRecentlyNotified(service)) {
      return;
    }
    send(service);
  }

  private static boolean hasRecentlyNotified(AppointmentService service) {
    return LAST_NOTIFIED_MAP.get(service) != null &&
        LAST_NOTIFIED_MAP.get(service).isAfter(Instant.now().minus(MAX_NOTIFICATION_RATE));
  }

  private static void send(AppointmentService service) {
    Message message = Message.creator(
        new PhoneNumber(ALERT_NUMBER_TO),
        new PhoneNumber(ALERT_NUMBER_FROM),
        String.format("Appointment found for %s!!!", service.getDescription())
    ).create();
    logger.info("Sent message alert {} with status {}", message.getSid(), message.getStatus());
    LAST_NOTIFIED_MAP.put(service, Instant.now());
  }
}
