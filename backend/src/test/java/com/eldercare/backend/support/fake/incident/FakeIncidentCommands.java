package com.eldercare.backend.support.fake.incident;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.api.IncidentCommandApi;
import com.eldercare.backend.incident.api.dto.RecordIncidentCommand;
import java.util.ArrayList;
import java.util.List;

/** Ghi lại lời gọi để consumer kiểm transaction; không cài máy trạng thái của production. */
public class FakeIncidentCommands implements IncidentCommandApi {
    private long nextId = 1;
    private final List<RecordIncidentCommand> recorded = new ArrayList<>();
    private final List<Long> incidents = new ArrayList<>();
    private final List<Long> alerts = new ArrayList<>();
    private final List<String> progressions = new ArrayList<>();

    public void nextId(long id) {
        nextId = id;
    }

    @Override
    public long record(RecordIncidentCommand command, CurrentUser user) {
        recorded.add(command);
        return nextId++;
    }

    @Override
    public void addProgression(long id, String type, String content, CurrentUser user) {
        progressions.add(id + ":" + type + ":" + content);
    }

    @Override
    public void acknowledge(long id, CurrentUser user) {
        incidents.add(id);
    }

    public void acknowledgeAlert(long id, CurrentUser user) {
        alerts.add(id);
    }

    public List<RecordIncidentCommand> recorded() {
        return List.copyOf(recorded);
    }

    public List<Long> acknowledgedIncidents() {
        return List.copyOf(incidents);
    }

    public List<Long> acknowledgedAlerts() {
        return List.copyOf(alerts);
    }

    public List<String> progressions() {
        return List.copyOf(progressions);
    }
}
