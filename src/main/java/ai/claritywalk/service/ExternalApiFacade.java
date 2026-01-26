package ai.claritywalk.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ExternalApiFacade {

    public Map<String, Object> getUserProfile(Map<String, Object> args) {
        // TODO call your user-profile service
        return Map.of(
                "name", "Martinsh",
                "preferred_language", "en",
                "goals", new String[]{"more calm", "daily walking"}
        );
    }

    public Map<String, Object> getTodaysPlan(Map<String, Object> args) {
        // TODO call program service
        return Map.of(
                "plan", "15 minutes easy walk",
                "focus", "notice breathing",
                "constraint", "keep it light"
        );
    }

    public Map<String, Object> getWeather(Map<String, Object> args) {
        // TODO call real weather API
        return Map.of(
                "temp_c", 2,
                "condition", "cloudy",
                "wind_kmh", 10
        );
    }

}
