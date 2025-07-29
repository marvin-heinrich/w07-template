package de.tum.aet.devops25.w07;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.aet.devops25.w07.controller.CanteenController;
import de.tum.aet.devops25.w07.dto.Dish;
import de.tum.aet.devops25.w07.dto.OpeningHours;
import de.tum.aet.devops25.w07.service.CanteenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CanteenController.class)
@AutoConfigureMockMvc
public class CanteenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Clock clock;

    @MockitoBean
    private CanteenService canteenService;

    @BeforeEach
    public void setup() {
        // Mock the clock to avoid flaky tests based on timing issues (because the service uses 'today')
        when(clock.instant()).thenReturn(Instant.parse("2025-05-08T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    public void testGetTodayMeals_ReturnsNoContent_WhenNoMealsAvailable() throws Exception {
        // Mock the service response instead of RestTemplate
        when(canteenService.getTodayMeals("mensa-garching")).thenReturn(List.of());

        // Act & Assert
        getList("/{canteenName}/today", HttpStatus.NO_CONTENT, Dish.class, "mensa-garching");
    }

    @Test
    public void testGetTodayMeals_ReturnsOkWithMeals() throws Exception {
        // Arrange
        List<Dish> expectedDishes = List.of(
            new Dish("Vegetarian Pasta", "Main Dish", List.of("VEGETARIAN")),
            new Dish("Salad", "Side Dish", List.of("VEGETARIAN"))
        );

        // Mock the service response
        when(canteenService.getTodayMeals("mensa-garching")).thenReturn(expectedDishes);

        // Act
        List<Dish> actualTodayDishes = getList("/{canteenName}/today", HttpStatus.OK, Dish.class, "mensa-garching");

        // Assert
        assertThat(actualTodayDishes).hasSize(2);
        var actualDish1 = actualTodayDishes.getFirst();
        assertThat(actualDish1.name()).isEqualTo("Vegetarian Pasta");
        assertThat(actualDish1.dish_type()).isEqualTo("Main Dish");
        var actualDish2 = actualTodayDishes.get(1);
        assertThat(actualDish2.name()).isEqualTo("Salad");
        assertThat(actualDish2.dish_type()).isEqualTo("Side Dish");
    }

    @Test
    public void testGetOpeningHours_ReturnsNoContent_WhenCanteenNotFound() throws Exception {
        // Mock the service response for unknown canteen
        when(canteenService.getOpeningHours("unknown-canteen")).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/{canteenName}/opening-hours", "unknown-canteen")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testGetOpeningHours_ReturnsOkWithOpeningHours_ForKnownCanteen() throws Exception {
        // Arrange
        OpeningHours expectedOpeningHours = new OpeningHours(
            "11:00 - 14:00",
            "11:00 - 14:00",
            "11:00 - 14:00",
            "11:00 - 14:00",
            "11:00 - 14:00",
            "Geschlossen",
            "Geschlossen"
        );

        // Mock the service response
        when(canteenService.getOpeningHours("mensa-garching")).thenReturn(expectedOpeningHours);

        // Act
        MvcResult result = mockMvc.perform(get("/{canteenName}/opening-hours", "mensa-garching")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Assert
        ObjectMapper mapper = new ObjectMapper();
        OpeningHours actualOpeningHours = mapper.readValue(result.getResponse().getContentAsString(), OpeningHours.class);
        
        assertThat(actualOpeningHours.monday()).isEqualTo("11:00 - 14:00");
        assertThat(actualOpeningHours.tuesday()).isEqualTo("11:00 - 14:00");
        assertThat(actualOpeningHours.wednesday()).isEqualTo("11:00 - 14:00");
        assertThat(actualOpeningHours.thursday()).isEqualTo("11:00 - 14:00");
        assertThat(actualOpeningHours.friday()).isEqualTo("11:00 - 14:00");
        assertThat(actualOpeningHours.saturday()).isEqualTo("Geschlossen");
        assertThat(actualOpeningHours.sunday()).isEqualTo("Geschlossen");
    }

    @Test
    public void testGetOpeningHours_ReturnsOkWithDifferentHours_ForDifferentCanteen() throws Exception {
        // Arrange
        OpeningHours expectedOpeningHours = new OpeningHours(
            "11:30 - 14:30",
            "11:30 - 14:30",
            "11:30 - 14:30",
            "11:30 - 14:30",
            "11:30 - 14:30",
            "Geschlossen",
            "Geschlossen"
        );

        // Mock the service response
        when(canteenService.getOpeningHours("mensa-leopoldstrasse")).thenReturn(expectedOpeningHours);

        // Act
        MvcResult result = mockMvc.perform(get("/{canteenName}/opening-hours", "mensa-leopoldstrasse")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Assert
        ObjectMapper mapper = new ObjectMapper();
        OpeningHours actualOpeningHours = mapper.readValue(result.getResponse().getContentAsString(), OpeningHours.class);
        
        assertThat(actualOpeningHours.monday()).isEqualTo("11:30 - 14:30");
        assertThat(actualOpeningHours.friday()).isEqualTo("11:30 - 14:30");
        assertThat(actualOpeningHours.saturday()).isEqualTo("Geschlossen");
    }

    private <T> List<T> getList(String path, HttpStatus expectedStatus, Class<T> listElementType, Object... uriVariables) throws Exception {
        MvcResult res = mockMvc.perform(get(path, uriVariables)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(expectedStatus.value()))
                .andReturn();

        if (expectedStatus.value() != 200) {
            if (res.getResponse().getContentType() != null && !res.getResponse().getContentType().equals("application/problem+json")) {
                assertThat(res.getResponse().getContentAsString()).isNullOrEmpty();
            }
            return null;
        }
        var mapper = new ObjectMapper();
        return mapper.readValue(res.getResponse().getContentAsString(), mapper.getTypeFactory().constructCollectionType(List.class, listElementType));
    }
}
