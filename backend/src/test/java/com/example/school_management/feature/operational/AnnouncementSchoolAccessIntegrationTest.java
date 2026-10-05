package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.auth.entity.*;
import com.example.school_management.feature.auth.repository.*;
import com.example.school_management.feature.membership.entity.*;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
import com.example.school_management.feature.operational.dto.RealTimeNotificationDto;
import com.example.school_management.feature.operational.repository.AnnouncementRepository;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.repository.SchoolRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
@Transactional
class AnnouncementSchoolAccessIntegrationTest {
    private static final String ROOT = "/api/v1/announcements";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired SchoolRepository schools;
    @Autowired SchoolMembershipRepository memberships;
    @Autowired UserRepository users;
    @Autowired StaffRepository staff;
    @Autowired TeacherRepository teachers;
    @Autowired StudentRepository students;
    @Autowired AnnouncementRepository announcements;
    @MockitoSpyBean CurrentSchoolResolver currentSchool;
    @MockitoBean SimpMessagingTemplate messaging;

    private School school, foreignSchool;
    private BaseUser admin, teacher, student, parent;
    private Staff staffUser;
    private long publicId, privateId, foreignId, classId, foreignClassId;

    @BeforeEach
    void setUp() {
        school = schools.findAll().get(0);
        foreignSchool = new School();
        foreignSchool.setName("Announcement School B");
        foreignSchool = schools.saveAndFlush(foreignSchool);
        doReturn(school).when(currentSchool).resolve();
        admin = users.findByEmail(DevFixtureLoader.ADMIN_EMAIL).orElseThrow();
        teacher = users.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow();
        student = users.findByEmail(DevFixtureLoader.STUDENT_EMAIL).orElseThrow();
        parent = users.findByEmail(DevFixtureLoader.PARENT_EMAIL).orElseThrow();
        staffUser = new Staff();
        staffUser.setRole(UserRole.STAFF);
        staffUser.setEmail(UUID.randomUUID() + "@fixtures.school.test");
        staffUser.setFirstName("Legacy"); staffUser.setLastName("Staff");
        staffUser.setPassword("unused"); staffUser.setStatus(Status.ACTIVE);
        staffUser = staff.saveAndFlush(staffUser);
        publicId = announcement(school, true);
        privateId = announcement(school, false);
        foreignId = announcement(foreignSchool, true);
        classId = clazz(school);
        foreignClassId = clazz(foreignSchool);
        jdbc.update("INSERT INTO class_teachers(class_id, teacher_id) VALUES (?, ?)", classId, teacher.getId());
        clearInvocations(messaging);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "TEACHER"})
    void createSetsCurrentSchoolAndIgnoresClientSchool(String role) throws Exception {
        var request = request(null);
        request.put("schoolId", foreignSchool.getId());
        long id = create(account(role), request);
        assertThat(jdbc.queryForObject("SELECT school_id FROM announcements WHERE id = ?", Long.class, id)).isEqualTo(school.getId());
        assertThat(jdbc.queryForObject("SELECT created_by_id FROM announcements WHERE id = ?", Long.class, id)).isEqualTo(account(role).getId());
        mvc.perform(put(ROOT + "/{id}", id).with(user(account(role).getEmail()).roles(role))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("schoolId", foreignSchool.getId(), "title", "Updated"))))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT school_id FROM announcements WHERE id = ?", Long.class, id)).isEqualTo(school.getId());
    }

    @ParameterizedTest
    @CsvSource({"ADMIN,absent", "ADMIN,STUDENT", "TEACHER,absent", "TEACHER,ADMIN", "STUDENT,absent", "STUDENT,GUARDIAN", "PARENT,absent", "PARENT,STUDENT"})
    void callerRequiresMatchingMembershipForEveryOperation(String role, String state) throws Exception {
        BaseUser caller = account(role);
        var membership = memberships.findByUserIdAndSchoolId(caller.getId(), school.getId()).orElseThrow();
        if (state.equals("absent")) memberships.delete(membership);
        else membership.setRoles(Set.of(MembershipRole.valueOf(state)));
        em.flush();
        for (var path : List.of(ROOT + "/" + publicId, ROOT + "/" + foreignId, ROOT + "/public")) {
            mvc.perform(get(path).with(user(caller.getEmail()).roles(role))).andExpect(status().isForbidden());
        }
        if (role.equals("ADMIN") || role.equals("TEACHER")) {
            mvc.perform(get(ROOT).with(user(caller.getEmail()).roles(role))).andExpect(status().isForbidden());
            mvc.perform(post(ROOT).with(user(caller.getEmail()).roles(role)).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(request(null)))).andExpect(status().isForbidden());
            mvc.perform(put(ROOT + "/{id}", publicId).with(user(caller.getEmail()).roles(role)).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Denied\"}")).andExpect(status().isForbidden());
            mvc.perform(delete(ROOT + "/{id}", publicId).with(user(caller.getEmail()).roles(role))).andExpect(status().isForbidden());
            if (role.equals("ADMIN")) mvc.perform(post(ROOT + "/{id}/publish", publicId).with(user(caller.getEmail()).roles(role))
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("userIds", List.of(student.getId())))))
                    .andExpect(status().isForbidden());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "TEACHER", "STUDENT", "PARENT"})
    void singleGetUsesSchoolAndPublicVisibility(String role) throws Exception {
        var caller = account(role);
        mvc.perform(get(ROOT + "/{id}", publicId).with(user(caller.getEmail()).roles(role))).andExpect(status().isOk());
        mvc.perform(get(ROOT + "/{id}", privateId).with(user(caller.getEmail()).roles(role)))
                .andExpect(role.equals("STUDENT") || role.equals("PARENT") ? status().isForbidden() : status().isOk());
        mvc.perform(get(ROOT + "/{id}", foreignId).with(user(caller.getEmail()).roles(role))).andExpect(status().isNotFound());
    }

    @Test
    void listsScopeRowsAndCountsBeforePagination() throws Exception {
        jdbc.update("UPDATE announcements SET start_date = CURRENT_TIMESTAMP + interval '1 day' WHERE id = ?", publicId);
        long currentPublic = announcement(school, true);
        long expired = announcement(school, true);
        jdbc.update("UPDATE announcements SET end_date = CURRENT_TIMESTAMP - interval '1 day' WHERE id = ?", expired);
        long total = jdbc.queryForObject("SELECT count(*) FROM announcements WHERE school_id = ?", Long.class, school.getId());
        String body = mvc.perform(get(ROOT).param("size", "1").with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(total))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(body).path("data").path("content").get(0).path("id").asLong();
        assertThat(jdbc.queryForObject("SELECT school_id FROM announcements WHERE id = ?", Long.class, id)).isEqualTo(school.getId());
        long publicTotal = jdbc.queryForObject("SELECT count(*) FROM announcements WHERE school_id = ? AND is_public AND (start_date IS NULL OR start_date <= CURRENT_TIMESTAMP) AND (end_date IS NULL OR end_date >= CURRENT_TIMESTAMP)", Long.class, school.getId());
        mvc.perform(get(ROOT + "/public").param("size", "1").with(user(student.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(publicTotal));
        String all = mvc.perform(get(ROOT + "/public").param("size", "100").with(user(parent.getEmail()).roles("PARENT")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Long> ids = new ArrayList<>();
        json.readTree(all).path("data").path("content").forEach(node -> ids.add(node.path("id").asLong()));
        assertThat(ids).contains(currentPublic).doesNotContain(publicId, privateId, foreignId, expired);
        long ownFiltered = announcement(school, false), foreignFiltered = announcement(foreignSchool, false);
        jdbc.update("UPDATE announcements SET importance = 'HIGH' WHERE id IN (?, ?)", ownFiltered, foreignFiltered);
        mvc.perform(get(ROOT).param("importance", "HIGH").param("isPublic", "false").with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(ownFiltered));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "TEACHER"})
    void foreignAnnouncementCannotBeUpdatedOrDeleted(String role) throws Exception {
        var caller = account(role);
        mvc.perform(put(ROOT + "/{id}", foreignId).with(user(caller.getEmail()).roles(role))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Denied\"}")).andExpect(status().isNotFound());
        mvc.perform(delete(ROOT + "/{id}", foreignId).with(user(caller.getEmail()).roles(role))).andExpect(status().isNotFound());
        assertThat(announcements.existsById(foreignId)).isTrue();
    }

    @Test
    void classTargetsAreCompletelyValidatedBeforeWrites() throws Exception {
        for (var ids : List.of(List.of(foreignClassId), List.of(classId, foreignClassId))) {
            var req = request("CLASSES"); req.put("targetClassIds", ids);
            rejectedCreate(teacher, req, 404);
        }
        var req = request("CLASSES"); req.put("targetClassIds", List.of(clazz(school)));
        rejectedCreate(teacher, req, 409);
    }

    @Test
    void classAudienceRequiresActiveEnrollmentAndStudentMembership() throws Exception {
        Student foreign = student("Foreign"); membership(foreign, foreignSchool, MembershipRole.STUDENT);
        Student wrongRole = student("Wrong"); membership(wrongRole, school, MembershipRole.GUARDIAN);
        Student inactive = student("Inactive"); membership(inactive, school, MembershipRole.STUDENT);
        for (var target : List.of(student, foreign, wrongRole, inactive)) {
            jdbc.update("INSERT INTO enrollments(student_id, class_id, status) VALUES (?, ?, ?)", target.getId(), classId,
                    target == inactive ? "WITHDRAWN" : "ACTIVE");
        }
        var req = request("CLASSES"); req.put("targetClassIds", List.of(classId));
        long id = create(teacher, req);
        assertRecipients(id, Set.of(student.getId()));
    }

    @ParameterizedTest
    @CsvSource({"ALL_TEACHERS,TEACHER", "ALL_STUDENTS,STUDENT"})
    void broadAudienceUsesMembershipAndPerUserRealtime(String target, String role) throws Exception {
        Teacher foreign = teacher("Foreign"); membership(foreign, foreignSchool, MembershipRole.valueOf(role));
        Teacher current = teacher("Current"); membership(current, school, MembershipRole.valueOf(role));
        // Audience truth is the School membership, regardless of global subtype/account role.
        long id = create(admin, request(target));
        Set<Long> expected = new HashSet<>(jdbc.queryForList("SELECT DISTINCT m.user_id FROM school_memberships m JOIN school_membership_roles r ON r.membership_id = m.id WHERE m.school_id = ? AND r.role = ?", Long.class, school.getId(), role));
        assertRecipients(id, expected);
        var destinations = ArgumentCaptor.forClass(String.class);
        var payloads = ArgumentCaptor.forClass(Object.class);
        verify(messaging, times(expected.size())).convertAndSend(destinations.capture(), payloads.capture());
        assertThat(destinations.getAllValues()).allSatisfy(destination -> assertThat(destination).startsWith("/queue/user/").endsWith("/notifications"));
        for (int i = 0; i < expected.size(); i++) {
            var dto = (RealTimeNotificationDto) payloads.getAllValues().get(i);
            assertThat(dto.getTargetUserIds()).hasSize(1);
            long recipient = dto.getTargetUserIds().iterator().next();
            assertThat(destinations.getAllValues().get(i)).isEqualTo("/queue/user/" + recipient + "/notifications");
            assertThat(expected).contains(recipient);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "TEACHER", "STAFF"})
    void specificCurrentSchoolUsersSucceedForSupportedCreators(String role) throws Exception {
        if (role.equals("STAFF")) singleSchool();
        var req = request("SPECIFIC_USERS"); req.put("targetUserIds", List.of(admin.getId(), teacher.getId(), student.getId(), parent.getId()));
        long id = create(account(role), req);
        assertRecipients(id, Set.of(admin.getId(), teacher.getId(), student.getId(), parent.getId()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void specificUsersValidateWholeSetEvenWithoutDelivery(boolean send) throws Exception {
        var foreign = teacher("Foreign"); membership(foreign, foreignSchool, MembershipRole.TEACHER);
        var unsupported = teacher("Unsupported");
        for (long bad : List.of(foreign.getId(), Long.MAX_VALUE, unsupported.getId(), staffUser.getId())) {
            var req = request("SPECIFIC_USERS"); req.put("targetUserIds", List.of(student.getId(), bad, parent.getId()));
            req.put("sendNotifications", send);
            rejectedCreate(admin, req, 404);
        }
    }

    @Test
    void publishPrevalidatesRecipientsAndScopesAnnouncement() throws Exception {
        var foreign = teacher("Foreign"); membership(foreign, foreignSchool, MembershipRole.TEACHER);
        long before = notificationCount();
        mvc.perform(post(ROOT + "/{id}/publish", foreignId).with(user(admin.getEmail()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("userIds", List.of(student.getId())))))
                .andExpect(status().isNotFound());
        for (long bad : List.of(foreign.getId(), Long.MAX_VALUE)) {
            mvc.perform(post(ROOT + "/{id}/publish", publicId).with(user(admin.getEmail()).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("userIds", List.of(student.getId(), bad, parent.getId())))))
                    .andExpect(status().isNotFound());
            assertThat(notificationCount()).isEqualTo(before);
        }
        verifyNoInteractions(messaging);
        mvc.perform(post(ROOT + "/{id}/publish", publicId).with(user(admin.getEmail()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("userIds", List.of(student.getId(), teacher.getId())))))
                .andExpect(status().isOk());
        assertRecipients(publicId, Set.of(student.getId(), teacher.getId()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALL_STAFF", "WHOLE_SCHOOL"})
    void staffAudiencesFailClosedInMultipleSchools(String target) throws Exception {
        rejectedCreate(admin, request(target), 409);
        var req = request(target); req.put("sendNotifications", false);
        rejectedCreate(admin, req, 409);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALL_STAFF", "WHOLE_SCHOOL"})
    void staffAudiencesWorkOnlyInProvenSingleSchool(String target) throws Exception {
        singleSchool();
        var extra = teacher("Multi-role"); membership(extra, school, MembershipRole.TEACHER);
        var member = memberships.findByUserIdAndSchoolId(extra.getId(), school.getId()).orElseThrow();
        member.setRoles(Set.of(MembershipRole.TEACHER, MembershipRole.GUARDIAN)); em.flush();
        long id = create(admin, request(target));
        Set<Long> expected = new HashSet<>(jdbc.queryForList("SELECT id FROM staff", Long.class));
        if (target.equals("WHOLE_SCHOOL")) expected.addAll(jdbc.queryForList("SELECT DISTINCT m.user_id FROM school_memberships m JOIN school_membership_roles r ON r.membership_id = m.id WHERE m.school_id = ? AND r.role IN ('TEACHER','STUDENT','GUARDIAN')", Long.class, school.getId()));
        assertRecipients(id, expected);
    }

    @Test
    void staffCallerFailsClosedWithMultipleOrZeroSchools() throws Exception {
        staffDenied();
        jdbc.update("TRUNCATE schools CASCADE"); em.clear();
        staffDenied();
    }

    @Test
    void staffSingleSchoolCompatibilityPreservesManagementAndPublish() throws Exception {
        singleSchool();
        var req = request("SPECIFIC_USERS"); req.put("targetUserIds", List.of(staffUser.getId(), student.getId()));
        long id = create(staffUser, req);
        assertRecipients(id, Set.of(staffUser.getId(), student.getId()));
        mvc.perform(get(ROOT + "/{id}", privateId).with(user(staffUser.getEmail()).roles("STAFF"))).andExpect(status().isOk());
        mvc.perform(get(ROOT).with(user(staffUser.getEmail()).roles("STAFF"))).andExpect(status().isOk());
        mvc.perform(get(ROOT + "/public").with(user(staffUser.getEmail()).roles("STAFF"))).andExpect(status().isOk());
        mvc.perform(put(ROOT + "/{id}", privateId).with(user(staffUser.getEmail()).roles("STAFF"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Staff updated\"}")).andExpect(status().isOk());
        mvc.perform(post(ROOT + "/{id}/publish", privateId).with(user(staffUser.getEmail()).roles("STAFF"))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("userIds", List.of(parent.getId())))))
                .andExpect(status().isOk());
        assertRecipients(privateId, Set.of(parent.getId()));
        mvc.perform(delete(ROOT + "/{id}", privateId).with(user(staffUser.getEmail()).roles("STAFF"))).andExpect(status().isOk());
    }

    @Test
    void publisherIdsFailClosedAndLegacyIdsAreHiddenInMultiSchool() throws Exception {
        jdbc.update("INSERT INTO staff_announcements(staff_id, announcement_id) VALUES (?, ?)", staffUser.getId(), publicId);
        var req = request(null); req.put("publisherIds", List.of(staffUser.getId()));
        rejectedCreate(admin, req, 409);
        mvc.perform(get(ROOT + "/{id}", publicId).with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.publisherIds").isEmpty());
        mvc.perform(put(ROOT + "/{id}", publicId).with(user(admin.getEmail()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("title", "Rejected", "publisherIds", List.of(staffUser.getId())))))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT title FROM announcements WHERE id = ?", String.class, publicId)).isEqualTo("Test announcement");
        mvc.perform(put(ROOT + "/{id}", publicId).with(user(admin.getEmail()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"publisherIds\":[]}"))
                .andExpect(status().isOk());
        em.flush();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_announcements WHERE announcement_id = ?", Long.class, publicId)).isZero();
    }

    @Test
    void publisherIdsPreserveSingleSchoolCompatibility() throws Exception {
        singleSchool();
        var req = request(null); req.put("publisherIds", List.of(staffUser.getId()));
        long id = create(admin, req);
        mvc.perform(get(ROOT + "/{id}", id).with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.publisherIds[0]").value(staffUser.getId()));
        mvc.perform(put(ROOT + "/{id}", id).with(user(admin.getEmail()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("publisherIds", List.of(staffUser.getId())))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.publisherIds[0]").value(staffUser.getId()));
    }

    @Test
    void unknownTargetFailsClosed() throws Exception {
        rejectedCreate(admin, request("UNKNOWN"), 409);
    }

    @Test
    void announcementAuditRetainsDatabaseHistoryWithoutGlobalRealtimeBroadcast() throws Exception {
        var req = request(null);
        req.put("sendNotifications", false);
        long id = create(admin, req);
        mvc.perform(put(ROOT + "/{id}", id).with(user(admin.getEmail()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Updated\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete(ROOT + "/{id}", id).with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk());
        em.flush();
        assertThat(jdbc.queryForList("SELECT event_type::text FROM audit_events WHERE entity_type = 'Announcement' AND entity_id = ? ORDER BY id", String.class, id))
                .containsExactly("ANNOUNCEMENT_CREATED", "ANNOUNCEMENT_UPDATED", "ANNOUNCEMENT_DELETED");
        verifyNoInteractions(messaging);
    }

    @ParameterizedTest
    @ValueSource(strings = {"targetClassIds", "targetUserIds", "publisherIds"})
    void nullTargetIdsAreNotFoundBeforeMutation(String field) throws Exception {
        if (field.equals("publisherIds")) singleSchool();
        var req = request(null);
        req.put(field, Collections.singletonList(null));
        rejectedCreate(admin, req, 404);
    }

    @Test
    void urgentAnnouncementRetainsHighRealtimePriority() throws Exception {
        var req = request("ALL_STUDENTS"); req.put("importance", "URGENT");
        create(admin, req);
        ArgumentCaptor<Object> payloads = ArgumentCaptor.forClass(Object.class);
        verify(messaging, atLeastOnce()).convertAndSend(anyString(), payloads.capture());
        assertThat(payloads.getAllValues().stream().map(RealTimeNotificationDto.class::cast)
                .filter(dto -> "USER_NOTIFICATION".equals(dto.getType())).toList())
                .isNotEmpty().allSatisfy(dto -> assertThat(dto.getPriority()).isEqualTo("HIGH"));
    }

    @Test
    void automaticStaffPublisherCannotBypassMultiSchoolPolicyWithAdminAccountRole() throws Exception {
        staffUser.setRole(UserRole.ADMIN);
        membership(staffUser, school, MembershipRole.ADMIN);
        rejectedCreate(staffUser, request(null), 409);
    }

    private void staffDenied() throws Exception {
        for (String path : List.of(ROOT, ROOT + "/public", ROOT + "/" + publicId, ROOT + "/" + foreignId)) {
            mvc.perform(get(path).with(user(staffUser.getEmail()).roles("STAFF"))).andExpect(status().isForbidden());
        }
        mvc.perform(post(ROOT).with(user(staffUser.getEmail()).roles("STAFF")).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request(null)))).andExpect(status().isForbidden());
        mvc.perform(put(ROOT + "/{id}", publicId).with(user(staffUser.getEmail()).roles("STAFF"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Denied\"}")).andExpect(status().isForbidden());
        mvc.perform(delete(ROOT + "/{id}", publicId).with(user(staffUser.getEmail()).roles("STAFF"))).andExpect(status().isForbidden());
        mvc.perform(post(ROOT + "/{id}/publish", publicId).with(user(staffUser.getEmail()).roles("STAFF"))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("userIds", List.of(student.getId())))))
                .andExpect(status().isForbidden());
    }

    private long create(BaseUser caller, Map<String, Object> req) throws Exception {
        String response = mvc.perform(post(ROOT).with(user(caller.getEmail()).roles(caller.getRole().name()))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.schoolId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).path("data").path("id").asLong();
    }

    private void rejectedCreate(BaseUser caller, Map<String, Object> req, int expected) throws Exception {
        long before = announcements.count(), notifications = notificationCount();
        mvc.perform(post(ROOT).with(user(caller.getEmail()).roles(caller.getRole().name()))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().is(expected));
        assertThat(announcements.count()).isEqualTo(before);
        assertThat(notificationCount()).isEqualTo(notifications);
        verifyNoInteractions(messaging);
    }

    private Map<String, Object> request(String target) {
        Map<String, Object> req = new HashMap<>(Map.of("title", "Created announcement", "body", "Body", "isPublic", true, "importance", "MEDIUM", "sendNotifications", true));
        if (target != null) req.put("targetType", target);
        return req;
    }

    private BaseUser account(String role) {
        return switch (role) { case "ADMIN" -> admin; case "TEACHER" -> teacher; case "STUDENT" -> student; case "PARENT" -> parent; default -> staffUser; };
    }

    private void singleSchool() {
        jdbc.update("DELETE FROM announcements WHERE school_id = ?", foreignSchool.getId());
        jdbc.update("DELETE FROM classes WHERE academic_year_id IN (SELECT id FROM academic_years WHERE school_id = ?)", foreignSchool.getId());
        jdbc.update("DELETE FROM academic_years WHERE school_id = ?", foreignSchool.getId());
        schools.delete(foreignSchool); em.flush();
        assertThat(schools.count()).isEqualTo(1);
    }

    private long announcement(School owner, boolean visible) {
        return jdbc.queryForObject("INSERT INTO announcements(title, body, is_public, created_by_id, school_id) VALUES ('Test announcement', 'Body', ?, ?, ?) RETURNING id", Long.class, visible, teacher.getId(), owner.getId());
    }

    private long clazz(School owner) {
        long year = jdbc.queryForObject("INSERT INTO academic_years(school_id, name, start_date, end_date) VALUES (?, ?, '2026-08-17', '2027-07-09') RETURNING id", Long.class, owner.getId(), "Year " + UUID.randomUUID());
        return jdbc.queryForObject("INSERT INTO classes(name, academic_year_id) VALUES ('Class', ?) RETURNING id", Long.class, year);
    }

    private Teacher teacher(String name) {
        Teacher target = new Teacher(); target.setRole(UserRole.TEACHER); target.setEmail(UUID.randomUUID() + "@fixtures.school.test");
        target.setFirstName(name); target.setLastName("Teacher"); target.setPassword("unused"); target.setStatus(Status.ACTIVE);
        return teachers.saveAndFlush(target);
    }

    private Student student(String name) {
        Student target = new Student(); target.setRole(UserRole.STUDENT); target.setEmail(UUID.randomUUID() + "@fixtures.school.test");
        target.setFirstName(name); target.setLastName("Student"); target.setPassword("unused"); target.setStatus(Status.ACTIVE);
        return students.saveAndFlush(target);
    }

    private void membership(BaseUser user, School owner, MembershipRole role) {
        var membership = new SchoolMembership(); membership.setUser(user); membership.setSchool(owner);
        membership.setRoles(Set.of(role)); membership.setStatus(MembershipStatus.INACTIVE);
        memberships.saveAndFlush(membership);
    }

    private long notificationCount() { return jdbc.queryForObject("SELECT count(*) FROM user_notifications", Long.class); }

    private void assertRecipients(long announcement, Set<Long> expected) {
        assertThat(jdbc.queryForList("SELECT user_id FROM user_notifications WHERE entity_type = 'ANNOUNCEMENT' AND entity_id = ?", Long.class, announcement))
                .containsExactlyInAnyOrderElementsOf(expected);
    }
}
