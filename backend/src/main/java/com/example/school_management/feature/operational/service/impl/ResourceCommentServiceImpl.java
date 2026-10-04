package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.academic.entity.LearningResource;
import com.example.school_management.feature.academic.repository.LearningResourceRepository;
import com.example.school_management.feature.academic.service.LearningResourceService;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.example.school_management.feature.operational.dto.CreateResourceCommentRequest;
import com.example.school_management.feature.operational.dto.ResourceCommentDto;
import com.example.school_management.feature.operational.entity.ResourceComment;
import com.example.school_management.feature.operational.repository.ResourceCommentRepository;
import com.example.school_management.feature.operational.service.ResourceCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ResourceCommentServiceImpl implements ResourceCommentService {

    private static final String NOT_AVAILABLE = "This learning resource is not available to you";

    private final ResourceCommentRepository repository;
    private final LearningResourceRepository learningResourceRepository;
    private final LearningResourceService learningResourceService;
    private final UserRepository userRepository;
    private final CurrentSchoolResolver currentSchoolResolver;
    private final SchoolMembershipRepository memberships;

    @Override
    public ResourceCommentDto create(CreateResourceCommentRequest request) {
        log.debug("Creating resource comment for resource {}", request.getResourceId());
        
        // Get current user
        BaseUser currentUser = getCurrentUser();
        
        // Get learning resource
        LearningResource resource = requireSchoolResource(request.getResourceId(), currentSchoolResolver.resolve().getId());
        requireVisible(resource);
        
        // Create comment
        ResourceComment comment = new ResourceComment();
        comment.setContent(request.getContent());
        comment.setOnResource(resource);
        comment.setCommentedBy(currentUser);
        
        ResourceComment saved = repository.save(comment);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceCommentDto get(Long id) {
        log.debug("Fetching resource comment {}", id);
        
        ResourceComment comment = requireSchoolComment(id, currentSchoolResolver.resolve().getId());
        requireVisible(comment.getOnResource());
        
        return mapToDto(comment);
    }

    @Override
    public void delete(Long id) {
        log.debug("Deleting resource comment {}", id);
        
        Long schoolId = currentSchoolResolver.resolve().getId();
        ResourceComment comment = requireSchoolComment(id, schoolId);
        
        // Check if current user is the comment author or has admin privileges
        BaseUser currentUser = getCurrentUser();
        if (!comment.getCommentedBy().getId().equals(currentUser.getId()) && 
            !hasMembershipRole(currentUser.getId(), schoolId, MembershipRole.ADMIN)) {
            throw new AccessDeniedException("You can only delete your own comments");
        }
        
        repository.delete(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResourceCommentDto> findByResourceId(Long resourceId, Pageable pageable) {
        Long schoolId = currentSchoolResolver.resolve().getId();
        requireSchoolResource(resourceId, schoolId);
        return repository.findByResourceIdAndSchoolId(resourceId, schoolId, publicOnly(), pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResourceCommentDto> findByUserId(Long userId, Pageable pageable) {
        Long schoolId = currentSchoolResolver.resolve().getId();
        requireSchoolCommenter(userId, schoolId);
        return repository.findByCommentedByUserIdAndSchoolId(userId, schoolId, publicOnly(), pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResourceCommentDto> list(Pageable pageable) {
        return repository.findAllVisibleBySchoolId(currentSchoolResolver.resolve().getId(), publicOnly(), pageable).map(this::mapToDto);
    }

    /*
     * A comment is visible exactly when its learning resource is: callers who see public resources
     * only never read, list or add comments on a private one, nor learn its title from a comment.
     */
    private void requireVisible(LearningResource resource) {
        if (!resource.isPublic() && !learningResourceService.seesPrivateResources()) {
            throw new AccessDeniedException(NOT_AVAILABLE);
        }
    }

    private boolean publicOnly() {
        return !learningResourceService.seesPrivateResources();
    }

    private LearningResource requireSchoolResource(Long id, Long schoolId) {
        return learningResourceRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning resource not found with id: " + id));
    }

    private ResourceComment requireSchoolComment(Long id, Long schoolId) {
        return repository.findByIdAndOnResourceSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource comment not found with id: " + id));
    }

    private void requireSchoolCommenter(Long userId, Long schoolId) {
        boolean visible = memberships.findByUserIdAndSchoolId(userId, schoolId)
                .map(membership -> membership.getRoles().stream().anyMatch(role ->
                        role == MembershipRole.STUDENT || role == MembershipRole.TEACHER || role == MembershipRole.ADMIN))
                .orElse(false);
        if (!visible) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
    }

    private boolean hasMembershipRole(Long userId, Long schoolId, MembershipRole role) {
        return memberships.findByUserIdAndSchoolId(userId, schoolId)
                .map(membership -> membership.getRoles().contains(role))
                .orElse(false);
    }

    private BaseUser getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private ResourceCommentDto mapToDto(ResourceComment comment) {
        return new ResourceCommentDto(
                comment.getId(),
                comment.getContent(),
                comment.getOnResource().getId(),
                comment.getOnResource().getTitle(),
                comment.getCommentedBy().getId(),
                comment.getCommentedBy().getFirstName() + " " + comment.getCommentedBy().getLastName(),
                comment.getCreatedAt()
        );
    }
}
