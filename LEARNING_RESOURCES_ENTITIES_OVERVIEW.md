# 📚 Learning Resources System - Entities & Components Overview

## 📋 Table of Contents
- [System Overview](#system-overview)
- [Core Entities](#core-entities)
- [Database Schema](#database-schema)
- [API Layer](#api-layer)
- [Frontend Components](#frontend-components)
- [Resource Types & Enums](#resource-types--enums)
- [Relationships & Associations](#relationships--associations)
- [File Management](#file-management)
- [Search & Filtering](#search--filtering)
- [Comments & Interactions](#comments--interactions)
- [Security & Access Control](#security--access-control)
- [Performance Optimizations](#performance-optimizations)

---

## 🏗️ System Overview

The Learning Resources System is a comprehensive educational content management platform that allows teachers to upload, organize, and share various types of learning materials with students. The system supports multiple resource types, advanced search capabilities, and interactive features like comments and view tracking.

### **Key Features**
- ✅ **Multi-format Support**: Videos, documents, images, audio, presentations, and links
- ✅ **Targeted Distribution**: Resources can be assigned to specific classes, courses, and teachers
- ✅ **Advanced Search**: Full-text search across titles and descriptions
- ✅ **Interactive Features**: Comments, view tracking, and download statistics
- ✅ **File Management**: Secure upload, storage, and access control
- ✅ **Public/Private Resources**: Flexible visibility settings
- ✅ **Real-time Updates**: WebSocket notifications for new resources

---

## 🗄️ Core Entities

### **1. LearningResource Entity**

#### **Entity Definition**
```java
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@EntityListeners(AuditingEntityListener.class)
public class LearningResource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @EqualsAndHashCode.Include
    private String url;
    @EqualsAndHashCode.Include
    private String title;

    @Lob
    @EqualsAndHashCode.Include
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", columnDefinition = "resource_type")
    @ColumnTransformer(write = "?::resource_type")
    @EqualsAndHashCode.Include
    private ResourceType type;

    @EqualsAndHashCode.Include
    private String thumbnailUrl;
    @EqualsAndHashCode.Include
    private Integer duration; // in minutes

    @EqualsAndHashCode.Include
    private boolean isPublic = true;
    @EqualsAndHashCode.Include
    private String status = "ACTIVE";
    
    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;
    
    @Column(name = "download_count", nullable = false)
    private Long downloadCount = 0L;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

#### **Key Properties**
- **id**: Primary key with auto-generation
- **title**: Resource title (required)
- **description**: Detailed description (Lob for large text)
- **url**: Resource URL or file path
- **type**: Resource type enum (VIDEO, DOCUMENT, IMAGE, AUDIO, LINK)
- **thumbnailUrl**: Preview image URL
- **duration**: Duration in minutes (for videos/audio)
- **isPublic**: Visibility setting (public/private)
- **status**: Resource status (ACTIVE, ARCHIVED, etc.)
- **viewCount**: Number of views (tracked automatically)
- **downloadCount**: Number of downloads (tracked automatically)
- **createdAt/updatedAt**: Audit timestamps

### **2. ResourceComment Entity**

#### **Entity Definition**
```java
@Data
@Entity
@Table(name = "resource_comments")
public class ResourceComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    @ManyToOne
    @JoinColumn(name = "on_resource_id", nullable = false)
    private LearningResource onResource;

    @ManyToOne
    @JoinColumn(name = "commented_by_id", nullable = false)
    private BaseUser commentedBy;

    @CreatedDate
    private LocalDateTime createdAt;
}
```

#### **Key Properties**
- **id**: Primary key
- **content**: Comment text content
- **onResource**: Reference to the learning resource
- **commentedBy**: User who made the comment
- **createdAt**: Comment timestamp

---

## 🗃️ Database Schema

### **Main Tables**

#### **learning_resources Table**
```sql
CREATE TABLE learning_resources (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    url VARCHAR(500) NOT NULL,
    type resource_type NOT NULL,
    thumbnail_url VARCHAR(500),
    duration INTEGER,
    is_public BOOLEAN DEFAULT TRUE,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    view_count BIGINT DEFAULT 0,
    download_count BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### **resource_comments Table**
```sql
CREATE TABLE resource_comments (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    on_resource_id BIGINT NOT NULL,
    commented_by_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_resource_comments_resource 
        FOREIGN KEY (on_resource_id) REFERENCES learning_resources(id) ON DELETE CASCADE,
    CONSTRAINT fk_resource_comments_user 
        FOREIGN KEY (commented_by_id) REFERENCES base_user(id) ON DELETE CASCADE
);
```

### **Junction Tables**

#### **learning_resource_teachers**
```sql
CREATE TABLE learning_resource_teachers (
    resource_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    PRIMARY KEY (resource_id, teacher_id),
    CONSTRAINT fk_lr_teachers_resource 
        FOREIGN KEY (resource_id) REFERENCES learning_resources(id) ON DELETE CASCADE,
    CONSTRAINT fk_lr_teachers_teacher 
        FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE CASCADE
);
```

#### **learning_resource_classes**
```sql
CREATE TABLE learning_resource_classes (
    resource_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    PRIMARY KEY (resource_id, class_id),
    CONSTRAINT fk_lr_classes_resource 
        FOREIGN KEY (resource_id) REFERENCES learning_resources(id) ON DELETE CASCADE,
    CONSTRAINT fk_lr_classes_class 
        FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE
);
```

#### **learning_resource_courses**
```sql
CREATE TABLE learning_resource_courses (
    resource_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    PRIMARY KEY (resource_id, course_id),
    CONSTRAINT fk_lr_courses_resource 
        FOREIGN KEY (resource_id) REFERENCES learning_resources(id) ON DELETE CASCADE,
    CONSTRAINT fk_lr_courses_course 
        FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);
```

### **Enum Types**

#### **resource_type Enum**
```sql
CREATE TYPE resource_type AS ENUM (
    'VIDEO',
    'DOCUMENT', 
    'IMAGE',
    'AUDIO',
    'LINK'
);
```

---

## 🔌 API Layer

### **DTOs (Data Transfer Objects)**

#### **LearningResourceDto**
```java
@Value
public class LearningResourceDto {
    Long id;
    String title;
    String description;
    String url;
    ResourceType type;
    String thumbnailUrl;
    Integer duration;
    Boolean isPublic;
    String status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    
    @JsonProperty("teacherIds")
    Set<Long> teacherIds;
    
    @JsonProperty("classIds")
    Set<Long> classIds;
    
    @JsonProperty("courseIds")
    Set<Long> courseIds;
    
    @JsonProperty("commentCount")
    Integer commentCount;
    
    @JsonProperty("viewCount")
    Long viewCount;
    
    @JsonProperty("downloadCount")
    Long downloadCount;
}
```

#### **CreateLearningResourceRequest**
```java
@Value
public class CreateLearningResourceRequest {
    @NotBlank
    String title;
    
    String description;
    
    @NotBlank
    String url;
    
    @NotNull
    ResourceType type;
    
    String thumbnailUrl;
    
    Integer duration;
    
    Boolean isPublic = true;
    
    Set<Long> teacherIds;
    
    Set<Long> classIds;
    
    Set<Long> courseIds;
}
```

### **Service Interface**

#### **LearningResourceService**
```java
public interface LearningResourceService {
    LearningResourceDto create(CreateLearningResourceRequest request);
    LearningResourceDto update(Long id, UpdateLearningResourceRequest request);
    void delete(Long id);
    LearningResourceDto get(Long id);
    Page<LearningResourceDto> list(Pageable pageable);
    Page<LearningResourceDto> findByType(ResourceType type, Pageable pageable);
    Page<LearningResourceDto> findByTeacherId(Long teacherId, Pageable pageable);
    Page<LearningResourceDto> findByClassId(Long classId, Pageable pageable);
    Page<LearningResourceDto> findByCourseId(Long courseId, Pageable pageable);
    Page<LearningResourceDto> searchByTitleOrDescription(String searchTerm, String unused, Pageable pageable);
    LearningResourceDto uploadResource(MultipartFile file, CreateLearningResourceRequest request);
    void addTargetClasses(Long resourceId, Set<Long> classIds);
    void removeTargetClasses(Long resourceId, Set<Long> classIds);
    void addTargetCourses(Long resourceId, Set<Long> courseIds);
    void removeTargetCourses(Long resourceId, Set<Long> courseIds);
    void addTeachers(Long resourceId, Set<Long> teacherIds);
    void removeTeachers(Long resourceId, Set<Long> teacherIds);
    void incrementViewCount(String filename);
}
```

### **REST Controller**

#### **LearningResourceController**
```java
@RestController
@RequestMapping("/api/v1/learning-resources")
@RequiredArgsConstructor
@Tag(name = "Learning Resources", description = "Endpoints for managing learning resources")
@SecurityRequirement(name = "bearerAuth")
public class LearningResourceController {
    
    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<LearningResourceDto>> create(
            @Valid @RequestBody CreateLearningResourceRequest request);
    
    @PostMapping("/upload")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<LearningResourceDto>> uploadResource(
            @RequestParam("file") MultipartFile file,
            @RequestParam("request") String requestJson);
    
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageDto<LearningResourceDto>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ResourceType type,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String search);
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<LearningResourceDto>> get(@PathVariable Long id);
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<LearningResourceDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLearningResourceRequest request);
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable Long id);
}
```

---

## 🎨 Frontend Components

### **TypeScript Interfaces**

#### **LearningResource Interface**
```typescript
export interface LearningResource {
  id: number;
  title: string;
  description: string;
  url?: string;
  filename?: string;
  type: ResourceType;
  thumbnailUrl?: string;
  duration?: number;
  isPublic: boolean;
  teacherIds: number[];
  classIds: number[];
  courseIds: number[];
  createdAt: string;
  updatedAt: string;
}
```

#### **ResourceType Enum**
```typescript
export enum ResourceType {
  VIDEO = 'VIDEO',
  DOCUMENT = 'DOCUMENT',
  PRESENTATION = 'PRESENTATION',
  AUDIO = 'AUDIO',
  IMAGE = 'IMAGE',
  LINK = 'LINK',
  OTHER = 'OTHER',
}
```

#### **Request/Response Interfaces**
```typescript
export interface CreateLearningResourceRequest {
  title: string;
  description: string;
  url?: string;
  type: ResourceType;
  thumbnailUrl?: string;
  duration?: number;
  isPublic: boolean;
  teacherIds?: number[];
  classIds?: number[];
  courseIds?: number[];
}

export interface LearningResourceResponse {
  content: LearningResource[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ResourceComment {
  id: number;
  content: string;
  resourceId: number;
  userId: number;
  userName: string;
  createdAt: string;
  updatedAt: string;
}
```

### **Main Components**

#### **LearningSpace.tsx**
```typescript
const LearningSpace = () => {
  const { user } = useAuth();
  const isStudent = user?.role === 'STUDENT';
  const isTeacher = user?.role === 'TEACHER';
  
  // State management
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedClass, setSelectedClass] = useState<number | null>(null);
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  
  // Data fetching
  const resourcesQuery = useLearningResources({
    size: 20,
    search: searchQuery || undefined,
    classId: selectedClass || undefined,
  });
  
  // Mutations
  const downloadMutation = useDownloadResource();
  const deleteMutation = useDeleteLearningResource();
  const updateMutation = useUpdateLearningResource();
  
  return (
    <div className="space-y-6">
      {/* Search and filters */}
      <div className="flex gap-4">
        <Input
          placeholder="Search resources..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
        {/* Class selector */}
        {/* View mode toggle */}
      </div>
      
      {/* Resources grid/list */}
      <div className={viewMode === 'grid' ? 'grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6' : 'space-y-4'}>
        {classResources.map((resource) => (
          <ResourceCard
            key={resource.id}
            resource={resource}
            onDownload={handleDownload}
            onEdit={handleEdit}
            onDelete={handleDelete}
          />
        ))}
      </div>
    </div>
  );
};
```

#### **ResourceViewDialog.tsx**
```typescript
interface ResourceViewDialogProps {
  resource: LearningResource | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onDownload?: (resource: LearningResource) => void;
  onEdit?: (resource: LearningResource) => void;
  onDelete?: (resource: LearningResource) => void;
}

export function ResourceViewDialog({
  resource,
  open,
  onOpenChange,
  onDownload,
  onEdit,
  onDelete
}: ResourceViewDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[90vh]">
        <DialogHeader>
          <DialogTitle>{resource?.title}</DialogTitle>
        </DialogHeader>
        
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Resource preview */}
          <div className="space-y-4">
            <ResourcePreview resource={resource} />
            <div className="flex gap-2">
              <Button onClick={() => onDownload?.(resource)}>
                <Download className="w-4 h-4 mr-2" />
                Download
              </Button>
              <Button variant="outline">
                <Share2 className="w-4 h-4 mr-2" />
                Share
              </Button>
            </div>
          </div>
          
          {/* Resource details and comments */}
          <div className="space-y-4">
            <Card>
              <CardContent className="pt-6">
                <div className="space-y-2">
                  <div className="flex items-center gap-2">
                    <Badge variant="secondary">{resource?.type}</Badge>
                    <Badge variant="outline">
                      <Eye className="w-3 h-3 mr-1" />
                      {resource?.viewCount || 0}
                    </Badge>
                  </div>
                  <p className="text-sm text-muted-foreground">
                    {resource?.description}
                  </p>
                </div>
              </CardContent>
            </Card>
            
            <ResourceComments resourceId={resource?.id} />
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
```

#### **ResourcePreview.tsx**
```typescript
export function ResourcePreview({ resource }: { resource: LearningResource }) {
  const renderPreview = () => {
    switch (resource.type) {
      case ResourceType.VIDEO:
        return <VideoPreview url={resource.url} />;
      case ResourceType.DOCUMENT:
        return <PDFPreview url={resource.url} />;
      case ResourceType.IMAGE:
        return <ImagePreview url={resource.url} />;
      case ResourceType.AUDIO:
        return <AudioPreview url={resource.url} />;
      case ResourceType.LINK:
        return <LinkPreview url={resource.url} />;
      default:
        return <DefaultPreview resource={resource} />;
    }
  };

  return (
    <div className="w-full h-64 bg-muted rounded-lg flex items-center justify-center">
      {renderPreview()}
    </div>
  );
}
```

---

## 📁 Resource Types & Enums

### **ResourceType Enum**

#### **Backend Enum**
```java
public enum ResourceType {
    VIDEO,      // Video files (MP4, AVI, MOV, etc.)
    DOCUMENT,   // PDF documents, Word files, etc.
    IMAGE,      // Image files (JPG, PNG, GIF, etc.)
    AUDIO,      // Audio files (MP3, WAV, etc.)
    LINK        // External links and URLs
}
```

#### **Frontend Enum**
```typescript
export enum ResourceType {
  VIDEO = 'VIDEO',
  DOCUMENT = 'DOCUMENT',
  PRESENTATION = 'PRESENTATION',
  AUDIO = 'AUDIO',
  IMAGE = 'IMAGE',
  LINK = 'LINK',
  OTHER = 'OTHER',
}
```

### **Resource Type Features**

#### **VIDEO Resources**
- **Supported Formats**: MP4, AVI, MOV, WebM
- **Features**: Duration tracking, thumbnail generation, streaming
- **Preview**: Embedded video player
- **Metadata**: Duration, resolution, file size

#### **DOCUMENT Resources**
- **Supported Formats**: PDF, DOC, DOCX, PPT, PPTX, TXT
- **Features**: Text search, page navigation, download tracking
- **Preview**: PDF viewer, document preview
- **Metadata**: Page count, file size, creation date

#### **IMAGE Resources**
- **Supported Formats**: JPG, PNG, GIF, WebP, SVG
- **Features**: Thumbnail generation, zoom, gallery view
- **Preview**: Image viewer with zoom controls
- **Metadata**: Dimensions, file size, format

#### **AUDIO Resources**
- **Supported Formats**: MP3, WAV, OGG, M4A
- **Features**: Duration tracking, waveform display, streaming
- **Preview**: Audio player with controls
- **Metadata**: Duration, bitrate, file size

#### **LINK Resources**
- **Features**: URL validation, link preview, external access
- **Preview**: Link preview with metadata
- **Metadata**: Domain, title, description (from URL)

---

## 🔗 Relationships & Associations

### **Entity Relationships**

#### **LearningResource Relationships**
```java
// Many-to-Many with Teacher (creator)
@ManyToMany
@JoinTable(
    name = "learning_resource_teachers",
    joinColumns = @JoinColumn(name = "resource_id"),
    inverseJoinColumns = @JoinColumn(name = "teacher_id")
)
private Set<Teacher> createdBy = new HashSet<>();

// Many-to-Many with ClassEntity
@ManyToMany
@JoinTable(
    name = "learning_resource_classes",
    joinColumns = @JoinColumn(name = "resource_id"),
    inverseJoinColumns = @JoinColumn(name = "class_id")
)
private Set<ClassEntity> targetClasses = new HashSet<>();

// Many-to-Many with Course
@ManyToMany
@JoinTable(
    name = "learning_resource_courses",
    joinColumns = @JoinColumn(name = "resource_id"),
    inverseJoinColumns = @JoinColumn(name = "course_id")
)
private Set<Course> targetCourses = new HashSet<>();

// One-to-Many with ResourceComment
@OneToMany(mappedBy = "onResource", cascade = CascadeType.ALL, orphanRemoval = true)
private Set<ResourceComment> comments = new HashSet<>();
```

### **Relationship Management Methods**

#### **Helper Methods**
```java
// Teacher management
public void addTeacher(Teacher teacher) {
    this.createdBy.add(teacher);
    teacher.getLearningResources().add(this);
}

public void removeTeacher(Teacher teacher) {
    this.createdBy.remove(teacher);
    teacher.getLearningResources().remove(this);
}

// Class management
public void addTargetClass(ClassEntity classEntity) {
    this.targetClasses.add(classEntity);
    classEntity.getLearningResources().add(this);
}

public void removeTargetClass(ClassEntity classEntity) {
    this.targetClasses.remove(classEntity);
    classEntity.getLearningResources().remove(this);
}

// Course management
public void addTargetCourse(Course course) {
    this.targetCourses.add(course);
    course.getLearningResources().add(this);
}

public void removeTargetCourse(Course course) {
    this.targetCourses.remove(course);
    course.getLearningResources().remove(this);
}
```

---

## 📂 File Management

### **File Upload Configuration**

#### **Upload Path Configuration**
```java
@Value("${app.file.upload.path:uploads/learning-resources}")
private String uploadPath;
```

#### **File Security Service**
```java
@Service
public class FileSecurityService {
    
    public void validateFile(MultipartFile file) throws FileSecurityException {
        // File type validation
        // File size validation
        // Malware scanning
        // Content validation
    }
    
    public String generateSecureFilename(String originalFilename) {
        // Generate secure, unique filename
        // Prevent path traversal attacks
        // Ensure file extension safety
    }
}
```

### **File Storage Structure**
```
uploads/
└── learning-resources/
    ├── videos/
    │   ├── 2024/
    │   │   ├── 01/
    │   │   │   ├── video_001.mp4
    │   │   │   └── video_002.mp4
    │   │   └── 02/
    ├── documents/
    │   ├── 2024/
    │   │   ├── 01/
    │   │   └── 02/
    ├── images/
    │   ├── 2024/
    │   │   ├── 01/
    │   │   └── 02/
    └── audio/
        ├── 2024/
        │   ├── 01/
        │   └── 02/
```

### **File Access Control**
- **Authentication Required**: All file access requires valid authentication
- **Authorization Checks**: Users can only access resources assigned to their classes/courses
- **Public Resources**: Public resources are accessible to all authenticated users
- **Private Resources**: Private resources are only accessible to assigned users

---

## 🔍 Search & Filtering

### **Search Implementation**

#### **Repository Search Methods**
```java
@Repository
public interface LearningResourceRepository extends JpaRepository<LearningResource, Long> {
    
    @Query("SELECT lr FROM LearningResource lr WHERE " +
           "LOWER(lr.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(lr.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<LearningResource> searchByTitleOrDescription(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    Page<LearningResource> findByType(ResourceType type, Pageable pageable);
    Page<LearningResource> findByTeacherId(Long teacherId, Pageable pageable);
    Page<LearningResource> findByClassId(Long classId, Pageable pageable);
    Page<LearningResource> findByCourseId(Long courseId, Pageable pageable);
    
    @Query("SELECT lr FROM LearningResource lr WHERE lr.isPublic = true")
    Page<LearningResource> findPublicResources(Pageable pageable);
}
```

#### **Advanced Search Features**
- **Full-text Search**: Search across title and description fields
- **Type Filtering**: Filter by resource type (VIDEO, DOCUMENT, etc.)
- **Teacher Filtering**: Filter by resource creator
- **Class Filtering**: Filter by target class
- **Course Filtering**: Filter by target course
- **Public/Private Filtering**: Filter by visibility setting
- **Date Range Filtering**: Filter by creation date range

### **Frontend Search Implementation**
```typescript
export function useLearningResources(filters: LearningResourceFilters) {
  return useQuery({
    queryKey: ['learning-resources', filters],
    queryFn: async () => {
      const params = new URLSearchParams();
      
      if (filters.search) params.append('search', filters.search);
      if (filters.type) params.append('type', filters.type);
      if (filters.teacherId) params.append('teacherId', filters.teacherId.toString());
      if (filters.classId) params.append('classId', filters.classId.toString());
      if (filters.courseId) params.append('courseId', filters.courseId.toString());
      if (filters.isPublic !== undefined) params.append('isPublic', filters.isPublic.toString());
      
      const response = await http.get(`/v1/learning-resources?${params}`);
      return response.data;
    }
  });
}
```

---

## 💬 Comments & Interactions

### **Comment System**

#### **ResourceComment Entity**
```java
@Data
@Entity
@Table(name = "resource_comments")
public class ResourceComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    @ManyToOne
    @JoinColumn(name = "on_resource_id", nullable = false)
    private LearningResource onResource;

    @ManyToOne
    @JoinColumn(name = "commented_by_id", nullable = false)
    private BaseUser commentedBy;

    @CreatedDate
    private LocalDateTime createdAt;
}
```

#### **Comment API Endpoints**
```java
@RestController
@RequestMapping("/api/v1/learning-resources/{resourceId}/comments")
public class ResourceCommentController {
    
    @PostMapping
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<ResourceCommentDto>> createComment(
            @PathVariable Long resourceId,
            @Valid @RequestBody CreateResourceCommentRequest request);
    
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageDto<ResourceCommentDto>>> getComments(
            @PathVariable Long resourceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size);
    
    @DeleteMapping("/{commentId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteComment(
            @PathVariable Long resourceId,
            @PathVariable Long commentId);
}
```

### **View & Download Tracking**

#### **Usage Statistics**
```java
@Service
public class LearningResourceServiceImpl implements LearningResourceService {
    
    @Override
    public void incrementViewCount(String filename) {
        LearningResource resource = repository.findByUrlContaining(filename)
            .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        
        resource.setViewCount(resource.getViewCount() + 1);
        repository.save(resource);
        
        // Send real-time notification
        notificationService.notifyResourceViewed(resource);
    }
    
    @Override
    public void incrementDownloadCount(Long resourceId) {
        LearningResource resource = repository.findById(resourceId)
            .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        
        resource.setDownloadCount(resource.getDownloadCount() + 1);
        repository.save(resource);
        
        // Send real-time notification
        notificationService.notifyResourceDownloaded(resource);
    }
}
```

---

## 🔒 Security & Access Control

### **Authentication & Authorization**

#### **Controller Security**
```java
@RestController
@RequestMapping("/api/v1/learning-resources")
@SecurityRequirement(name = "bearerAuth")
public class LearningResourceController {
    
    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<LearningResourceDto>> create(
            @Valid @RequestBody CreateLearningResourceRequest request);
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<LearningResourceDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLearningResourceRequest request);
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable Long id);
}
```

### **Access Control Rules**

#### **Resource Access Matrix**
| User Role | Create | Read | Update | Delete | Comment |
|-----------|--------|------|--------|--------|---------|
| **ADMIN** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **TEACHER** | ✅ | ✅ | ✅* | ✅* | ✅ |
| **STUDENT** | ❌ | ✅** | ❌ | ❌ | ✅ |
| **PARENT** | ❌ | ✅** | ❌ | ❌ | ❌ |

*Only for resources they created
**Only for resources assigned to their classes/courses

#### **Resource Visibility Rules**
- **Public Resources**: Accessible to all authenticated users
- **Private Resources**: Only accessible to assigned users (teachers, students in target classes)
- **Class-specific Resources**: Only accessible to students enrolled in those classes
- **Course-specific Resources**: Only accessible to students taking those courses

---

## ⚡ Performance Optimizations

### **Database Optimizations**

#### **Strategic Indexes**
```sql
-- Search optimization
CREATE INDEX idx_learning_resources_title ON learning_resources USING gin(to_tsvector('english', title));
CREATE INDEX idx_learning_resources_description ON learning_resources USING gin(to_tsvector('english', description));

-- Filter optimization
CREATE INDEX idx_learning_resources_type ON learning_resources(type);
CREATE INDEX idx_learning_resources_status ON learning_resources(status);
CREATE INDEX idx_learning_resources_is_public ON learning_resources(is_public);
CREATE INDEX idx_learning_resources_created_at ON learning_resources(created_at);

-- Relationship optimization
CREATE INDEX idx_lr_teachers_teacher_id ON learning_resource_teachers(teacher_id);
CREATE INDEX idx_lr_classes_class_id ON learning_resource_classes(class_id);
CREATE INDEX idx_lr_courses_course_id ON learning_resource_courses(course_id);
```

#### **Query Optimization**
```java
@Repository
public interface LearningResourceRepository extends JpaRepository<LearningResource, Long> {
    
    @Query("SELECT lr FROM LearningResource lr " +
           "LEFT JOIN FETCH lr.createdBy " +
           "LEFT JOIN FETCH lr.targetClasses " +
           "LEFT JOIN FETCH lr.targetCourses " +
           "WHERE lr.id = :id")
    Optional<LearningResource> findByIdWithRelations(@Param("id") Long id);
    
    @Query("SELECT lr FROM LearningResource lr " +
           "WHERE lr.isPublic = true " +
           "ORDER BY lr.createdAt DESC")
    Page<LearningResource> findPublicResourcesSorted(Pageable pageable);
}
```

### **Caching Strategy**

#### **Redis Caching**
```java
@Service
public class LearningResourceServiceImpl implements LearningResourceService {
    
    @Cacheable(value = "learning-resources", key = "#id")
    public LearningResourceDto get(Long id) {
        // Implementation
    }
    
    @CacheEvict(value = "learning-resources", key = "#id")
    public LearningResourceDto update(Long id, UpdateLearningResourceRequest request) {
        // Implementation
    }
    
    @CacheEvict(value = "learning-resources", allEntries = true)
    public void delete(Long id) {
        // Implementation
    }
}
```

### **Frontend Optimizations**

#### **Lazy Loading**
```typescript
// Lazy load resource preview components
const VideoPreview = lazy(() => import('./VideoPreview'));
const PDFPreview = lazy(() => import('./PDFPreview'));
const ImagePreview = lazy(() => import('./ImagePreview'));

// Virtual scrolling for large resource lists
import { FixedSizeList as List } from 'react-window';

export function ResourceList({ resources }: { resources: LearningResource[] }) {
  return (
    <List
      height={600}
      itemCount={resources.length}
      itemSize={200}
      itemData={resources}
    >
      {ResourceItem}
    </List>
  );
}
```

#### **Image Optimization**
```typescript
// Lazy loading images with placeholder
export function ResourceThumbnail({ resource }: { resource: LearningResource }) {
  const [loaded, setLoaded] = useState(false);
  
  return (
    <div className="relative w-full h-48 bg-muted rounded-lg overflow-hidden">
      {!loaded && (
        <div className="absolute inset-0 flex items-center justify-center">
          <div className="animate-pulse bg-muted-foreground/20 w-full h-full" />
        </div>
      )}
      <img
        src={resource.thumbnailUrl}
        alt={resource.title}
        className={`w-full h-full object-cover transition-opacity duration-300 ${
          loaded ? 'opacity-100' : 'opacity-0'
        }`}
        onLoad={() => setLoaded(true)}
        loading="lazy"
      />
    </div>
  );
}
```

---

## 📊 System Metrics & Analytics

### **Usage Statistics**

#### **Resource Metrics**
- **View Count**: Total number of resource views
- **Download Count**: Total number of resource downloads
- **Comment Count**: Total number of comments per resource
- **Popular Resources**: Most viewed/downloaded resources
- **Resource Types Distribution**: Breakdown by resource type

#### **User Engagement Metrics**
- **Active Users**: Users who have viewed/downloaded resources
- **Resource Creation Rate**: New resources created per time period
- **Search Queries**: Most common search terms
- **Class Engagement**: Resource usage by class/course

### **Performance Metrics**
- **Search Response Time**: Average time for search queries
- **File Upload Time**: Average time for file uploads
- **Preview Load Time**: Average time for resource previews
- **API Response Time**: Average time for API endpoints

---

## 🔮 Future Enhancements

### **Planned Features**

#### **Advanced Content Management**
- **Resource Versioning**: Track changes and maintain versions
- **Resource Collections**: Group related resources together
- **Resource Templates**: Predefined resource structures
- **Bulk Operations**: Mass upload and management

#### **Enhanced Search & Discovery**
- **AI-Powered Search**: Semantic search capabilities
- **Recommendation Engine**: Suggest relevant resources
- **Tag System**: Flexible tagging for better organization
- **Advanced Filters**: Date range, file size, duration filters

#### **Collaboration Features**
- **Resource Sharing**: Share resources between teachers
- **Collaborative Editing**: Multiple users editing resources
- **Resource Reviews**: Peer review system for resources
- **Resource Ratings**: User rating system

#### **Analytics & Reporting**
- **Usage Analytics**: Detailed usage reports
- **Performance Dashboards**: Real-time performance metrics
- **Resource Effectiveness**: Track learning outcomes
- **Custom Reports**: Configurable reporting system

---

## 📝 Conclusion

The Learning Resources System provides a comprehensive platform for educational content management with robust entity relationships, advanced search capabilities, and interactive features. The system is designed for scalability, security, and user experience, making it suitable for educational institutions of all sizes.

The modular architecture allows for easy extension and customization, while the comprehensive API and frontend components provide a solid foundation for building advanced learning management features.

---

*Last Updated: January 2024*
*Version: 1.0.0*
*Author: School Management Development Team*
