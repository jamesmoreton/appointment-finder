package com.jamesmoreton;

enum AppointmentService {
  UK_PASSPORT_PREMIUM(
      "UK Passport Service (Online Premium)",
      "https://www.passport.service.gov.uk/urgent/",
      "Sorry, there are no available appointments – Apply for a passport – GOV.UK"
  ),
  UK_PASSPORT_FAST_TRACK(
      "UK Passport Service (1 week Fast Track)",
      "https://www.passportappointment.service.gov.uk/messages/AppointmentsAvailability.html",
      "Appointments Unavailable"
  );

  private final String description;
  private final String url;
  private final String noAppointmentsText;

  AppointmentService(String description, String url, String noAppointmentsText) {
    this.description = description;
    this.url = url;
    this.noAppointmentsText = noAppointmentsText;
  }

  public String getDescription() {
    return description;
  }

  public String getUrl() {
    return url;
  }

  public String getNoAppointmentsText() {
    return noAppointmentsText;
  }
}
