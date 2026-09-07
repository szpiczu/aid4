package pl.informatysta.aid4.s01e02;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PowerPlantFetcherTest {

    @TempDir
    Path tempDir;

    @SuppressWarnings("unchecked")
    @Test
    void fetch_mapsJsonAndCallsExpectedUri() throws Exception {
        String json = """
                {
                  "power_plants": {
                    "Alpha": {"is_active": true, "power": "120 MW", "code": "PP-A"},
                    "Beta": {"is_active": false, "power": "80 MW", "code": "PP-B"}
                  }
                }
                """;

        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.body()).thenReturn(json);

        HttpClient httpClient = mock(HttpClient.class);
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        when(httpClient.send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        Path cacheFile = tempDir.resolve("power_plants.cache");
        PowerPlantFetcher fetcher = new PowerPlantFetcher(httpClient, "dummy-key", cacheFile, false);
        List<PowerPlant> plants = fetcher.fetch();

        String uri = requestCaptor.getValue().uri().toString();
        assertTrue(uri.contains("dummy-key"));
        assertTrue(uri.contains("findhim_locations.json"));

        assertEquals(2, plants.size());

        PowerPlant alpha = plants.get(0);
        assertEquals("Alpha", alpha.name());
        assertTrue(alpha.isActive());
        assertEquals("120 MW", alpha.power());
        assertEquals("PP-A", alpha.code());

        PowerPlant beta = plants.get(1);
        assertEquals("Beta", beta.name());
        assertFalse(beta.isActive());
        assertEquals("80 MW", beta.power());
        assertEquals("PP-B", beta.code());
    }

    @SuppressWarnings("unchecked")
    @Test
    void fetch_usesCacheWhenFileExists() throws Exception {
        String json = """
                {
                  "power_plants": {
                    "Cached": {"is_active": true, "power": "50 MW", "code": "PWR-CACHE"}
                  }
                }
                """;
        Path cacheFile = tempDir.resolve("power_plants.cache");
        Files.writeString(cacheFile, json);

        HttpClient httpClient = mock(HttpClient.class);
        PowerPlantFetcher fetcher = new PowerPlantFetcher(httpClient, "dummy-key", cacheFile, false);
        List<PowerPlant> plants = fetcher.fetch();

        assertEquals(1, plants.size());
        assertEquals("Cached", plants.get(0).name());
        assertTrue(plants.get(0).isActive());
        assertEquals("50 MW", plants.get(0).power());
        assertEquals("PWR-CACHE", plants.get(0).code());
        verify(httpClient, never()).send(any(), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void fetch_writesCacheOnMiss() throws Exception {
        Path cacheFile = tempDir.resolve("power_plants.cache");
        String json = """
                {
                  "power_plants": {
                    "Remote": {"is_active": false, "power": "10 MW", "code": "PWR-REMOTE"}
                  }
                }
                """;

        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.body()).thenReturn(json);

        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        PowerPlantFetcher fetcher = new PowerPlantFetcher(httpClient, "dummy-key", cacheFile, false);
        List<PowerPlant> plants = fetcher.fetch();

        assertEquals(1, plants.size());
        assertEquals("Remote", plants.get(0).name());
        assertTrue(Files.exists(cacheFile));
        String cached = Files.readString(cacheFile);
        assertTrue(cached.contains("Remote"));
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void fetch_ignoresCacheWhenForceRefresh() throws Exception {
        String cachedJson = """
                {
                  "power_plants": {
                    "Stale": {"is_active": true, "power": "1 MW", "code": "PWR-STALE"}
                  }
                }
                """;
        Path cacheFile = tempDir.resolve("power_plants.cache");
        Files.writeString(cacheFile, cachedJson);

        String freshJson = """
                {
                  "power_plants": {
                    "Fresh": {"is_active": true, "power": "99 MW", "code": "PWR-FRESH"}
                  }
                }
                """;

        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.body()).thenReturn(freshJson);

        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        PowerPlantFetcher fetcher = new PowerPlantFetcher(httpClient, "dummy-key", cacheFile, true);
        List<PowerPlant> plants = fetcher.fetch();

        assertEquals(1, plants.size());
        assertEquals("Fresh", plants.get(0).name());
        assertEquals("PWR-FRESH", plants.get(0).code());
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }
}