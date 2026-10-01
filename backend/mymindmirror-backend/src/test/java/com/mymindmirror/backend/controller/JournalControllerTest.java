package com.mymindmirror.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.mapper.JournalMapper;
import com.mymindmirror.backend.model.JournalEntry;
import com.mymindmirror.backend.model.KeyPhrase;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.JournalEntryRequest;
import com.mymindmirror.backend.payload.response.JournalEntryResponse;
import com.mymindmirror.backend.payload.response.MoodDataResponse;
import com.mymindmirror.backend.payload.response.PageResponse;
import com.mymindmirror.backend.security.JwtUtil;
import com.mymindmirror.backend.security.services.UserDetailsImpl;
import com.mymindmirror.backend.service.JournalService;
import com.mymindmirror.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JournalController.class)
class JournalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JournalService journalService;

    @MockitoBean
    private JournalMapper journalMapper;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private UserService userService;  // needed for @CurrentUser resolver

    @MockitoBean
    private JwtUtil jwtUtil;

    private User currentUser;
    private UUID userId;
    private UUID entryId;
    private JournalEntry mockEntry;
    private JournalEntryResponse mockResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        entryId = UUID.randomUUID();

        currentUser = new User();
        currentUser.setId(userId);
        currentUser.setUsername("testuser");
        currentUser.setEmail("test@example.com");
        currentUser.setPasswordHash("hashed");

        // Mock UserService to return our user when resolver asks
        when(userService.findById(eq(userId))).thenReturn(Optional.of(currentUser));

        // Set up SecurityContext for @CurrentUser resolver
        UserDetailsImpl userDetails = new UserDetailsImpl(
                userId, currentUser.getUsername(), currentUser.getEmail(), currentUser.getPasswordHash(), null);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Prepare mock data
        mockEntry = new JournalEntry();
        mockEntry.setId(entryId);
        mockEntry.setUser(currentUser);
        mockEntry.setEntryDate(LocalDate.now());
        mockEntry.setCreationTimestamp(LocalDateTime.now());
        mockEntry.setRawText("Decrypted text");
        mockEntry.setMoodScore(0.8);
        mockEntry.setSummary("Test summary");
        mockEntry.setKeyPhrases(List.of(new KeyPhrase("test", mockEntry)));

        mockResponse = new JournalEntryResponse(
                entryId,
                null,
                LocalDate.now(),
                LocalDateTime.now(),
                "Decrypted text",
                0.8,
                Map.of("joy", 0.5),
                List.of("concern1"),
                "Test summary",
                List.of("tip1"),
                List.of("test"));
    }

    // ===== CREATE =====

    @Test
    void createJournalEntry_shouldReturn201() throws Exception {
        JournalEntryRequest request = new JournalEntryRequest("My journal text");
        when(journalService.saveJournalEntry(any(User.class), anyString())).thenReturn(mockEntry);
        when(journalMapper.toResponse(any(JournalEntry.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/journal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(entryId.toString()))
                .andExpect(jsonPath("$.rawText").value("Decrypted text"));
    }

    @Test
    void createJournalEntry_shouldReturn400_whenInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/api/journal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ===== UPDATE =====

    @Test
    void updateJournalEntry_shouldReturn200() throws Exception {
        JournalEntryRequest request = new JournalEntryRequest("Updated text");
        when(journalService.updateJournalEntry(eq(entryId), any(User.class), anyString())).thenReturn(mockEntry);
        when(journalMapper.toResponse(any(JournalEntry.class))).thenReturn(mockResponse);

        mockMvc.perform(put("/api/journal/{id}", entryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(entryId.toString()));
    }

    // ===== GET BY ID =====

    @Test
    void getJournalEntry_shouldReturn200_whenFound() throws Exception {
        when(journalService.getJournalEntryById(entryId)).thenReturn(Optional.of(mockEntry));
        when(journalMapper.toResponse(any(JournalEntry.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/journal/{id}", entryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(entryId.toString()));
    }

    @Test
    void getJournalEntry_shouldReturn404_whenNotFound() throws Exception {
        when(journalService.getJournalEntryById(entryId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/journal/{id}", entryId))
                .andExpect(status().isNotFound());
    }

    // ===== DELETE =====

    @Test
    void deleteJournalEntry_shouldReturn204() throws Exception {
        doNothing().when(journalService).deleteJournalEntry(eq(entryId), any(User.class));

        mockMvc.perform(delete("/api/journal/{id}", entryId))
                .andExpect(status().isNoContent());
    }

    // ===== HISTORY =====

    @Test
    void getJournalHistory_shouldReturn200() throws Exception {
        List<JournalEntry> entries = List.of(mockEntry);
        // The controller uses DateUtil.parseHistoryDateRange -> returns LocalDate
        when(journalService.getJournalEntriesForUser(any(User.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(entries);
        when(journalMapper.toResponse(any(JournalEntry.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/journal/history")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getJournalHistory_shouldUseFallbackDates_whenParamsOmitted() throws Exception {
        List<JournalEntry> entries = List.of(mockEntry);
        when(journalService.getJournalEntriesForUser(any(User.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(entries);
        when(journalMapper.toResponse(any(JournalEntry.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/journal/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ===== MOOD DATA =====

    @Test
    void getMoodData_shouldReturn200() throws Exception {
        List<MoodDataResponse> moodData = List.of(new MoodDataResponse(LocalDate.now(), 0.8));
        when(journalService.getMoodDataForChart(any(User.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(moodData);

        mockMvc.perform(get("/api/journal/mood-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ===== TRENDS =====

    @Test
    void getJournalTrends_shouldReturn200() throws Exception {
        Map<String, Long> trends = Map.of("test", 5L);
        //  Correct signature: (User, LocalDate, LocalDate, int)
        when(journalService.getTopKeyPhrases(any(User.class), any(LocalDate.class), any(LocalDate.class), anyInt()))
                .thenReturn(trends);

        mockMvc.perform(get("/api/journal/trends")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.test").value(5));
    }

    // ===== SEARCH =====

    @Test
    void searchByKeyword_shouldReturn200() throws Exception {
        List<JournalEntry> entries = List.of(mockEntry);
        when(journalService.searchJournalEntriesByKeyword(any(User.class), anyString())).thenReturn(entries);
        when(journalMapper.toResponse(any(JournalEntry.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/journal/search/keyword")
                        .param("keyword", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void searchByMood_shouldReturn200() throws Exception {
        List<JournalEntry> entries = List.of(mockEntry);
        when(journalService.searchJournalEntriesByMoodScore(any(User.class), anyDouble(), anyDouble()))
                .thenReturn(entries);
        when(journalMapper.toResponse(any(JournalEntry.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/journal/search/mood")
                        .param("minMood", "0.5")
                        .param("maxMood", "1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void searchByMood_shouldReturn400_whenMinGreaterThanMax() throws Exception {
        mockMvc.perform(get("/api/journal/search/mood")
                        .param("minMood", "1.0")
                        .param("maxMood", "0.5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchSemantic_shouldReturn200() throws Exception {
        List<JournalEntry> entries = List.of(mockEntry);
        when(journalService.searchJournalEntriesSemantically(any(User.class), anyString())).thenReturn(entries);
        when(journalMapper.toResponse(any(JournalEntry.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/journal/search/semantic")
                        .param("concept", "watermelon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ===== KEY PHRASES =====

    @Test
    void getKeyPhraseFrequencies_shouldReturn200() throws Exception {
        Map<String, Long> freq = Map.of("test", 10L);
        when(journalService.getKeyPhraseFrequencies(any(User.class))).thenReturn(freq);

        mockMvc.perform(get("/api/journal/key-phrases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.test").value(10));
    }

    // ===== PAGINATED HISTORY =====

    @Test
    void getJournalHistoryPaginated_shouldReturn200() throws Exception {
        PageResponse<JournalEntryResponse> pageResponse = PageResponse.<JournalEntryResponse>builder()
                .content(List.of(mockResponse))
                .pageNumber(0)
                .pageSize(20)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(journalService.getJournalEntriesPageResponse(any(User.class), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/journal/history/paginated")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }


    @Test
    void searchByMood_shouldUseDefaults_whenNoParams() throws Exception {
        // No parameters → min=-1.0, max=1.0
        List<JournalEntry> entries = List.of(mockEntry);
        when(journalService.searchJournalEntriesByMoodScore(any(User.class), eq(-1.0), eq(1.0)))
                .thenReturn(entries);
        when(journalMapper.toResponse(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/journal/search/mood"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void searchByMood_shouldUseMinDefault_whenMaxProvided() throws Exception {
        // Only max parameter → min=-1.0, max=0.8
        List<JournalEntry> entries = List.of(mockEntry);
        when(journalService.searchJournalEntriesByMoodScore(any(User.class), eq(-1.0), eq(0.8)))
                .thenReturn(entries);
        when(journalMapper.toResponse(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/journal/search/mood")
                        .param("maxMood", "0.8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void searchByMood_shouldUseMaxDefault_whenMinProvided() throws Exception {
        // Only min parameter → min=0.2, max=1.0
        List<JournalEntry> entries = List.of(mockEntry);
        when(journalService.searchJournalEntriesByMoodScore(any(User.class), eq(0.2), eq(1.0)))
                .thenReturn(entries);
        when(journalMapper.toResponse(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/journal/search/mood")
                        .param("minMood", "0.2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}