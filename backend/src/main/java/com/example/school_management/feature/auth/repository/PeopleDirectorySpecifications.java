package com.example.school_management.feature.auth.repository;

import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Parent;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.SchoolMembership;
import org.springframework.data.jpa.domain.Specification;

/** Directory membership includes administrative history, regardless of membership status. */
public final class PeopleDirectorySpecifications {
    private PeopleDirectorySpecifications() {}

    public static Specification<Student> studentsInSchool(Long schoolId) {
        return membership(schoolId, MembershipRole.STUDENT);
    }

    public static Specification<Teacher> teachersInSchool(Long schoolId) {
        return membership(schoolId, MembershipRole.TEACHER);
    }

    public static Specification<Parent> guardiansInSchool(Long schoolId) {
        return membership(schoolId, MembershipRole.GUARDIAN);
    }

    private static <T extends BaseUser> Specification<T> membership(Long schoolId, MembershipRole role) {
        return (root, query, cb) -> {
            var membership = query.subquery(Long.class);
            var member = membership.from(SchoolMembership.class);
            membership.select(member.get("id")).where(
                    cb.equal(member.get("user").get("id"), root.get("id")),
                    cb.equal(member.get("school").get("id"), schoolId),
                    cb.equal(member.join("roles"), role));
            return cb.exists(membership);
        };
    }
}
