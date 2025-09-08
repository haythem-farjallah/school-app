import { useMemo, useState } from "react";
import toast from "react-hot-toast";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { 
  Users, 
  GraduationCap, 
  UserCheck, 
  BookOpen, 
  Settings, 
  Shield,
  Search,
  Save,
  RotateCcw
} from "lucide-react";
import { usePermissionCatalogue, useRoleDefaults, useUpdateRoleDefaults } from "@/features/admin/permissions/hooks";

const ROLES = ["ADMIN", "TEACHER", "STUDENT", "PARENT", "STAFF"] as const;
type Role = typeof ROLES[number];

// Group permissions by domain for better UX
const PERMISSION_GROUPS = {
  "Student Management": {
    icon: GraduationCap,
    color: "bg-blue-100 text-blue-800",
    permissions: ["STUDENT_READ", "STUDENT_CREATE", "STUDENT_UPDATE", "STUDENT_DELETE"]
  },
  "Teacher Management": {
    icon: UserCheck,
    color: "bg-green-100 text-green-800", 
    permissions: ["TEACHER_READ", "TEACHER_CREATE", "TEACHER_UPDATE", "TEACHER_DELETE"]
  },
  "Academic Operations": {
    icon: BookOpen,
    color: "bg-purple-100 text-purple-800",
    permissions: ["GRADE_READ", "GRADE_WRITE", "COURSE_MANAGE", "CLASS_MANAGE"]
  },
  "System Administration": {
    icon: Settings,
    color: "bg-orange-100 text-orange-800",
    permissions: ["PERMISSIONS_MANAGE", "SYSTEM_ADMIN", "USER_ADMIN"]
  }
} as const;

const ROLE_COLORS: Record<Role, string> = {
  ADMIN: "bg-red-100 text-red-800 border-red-200",
  TEACHER: "bg-green-100 text-green-800 border-green-200", 
  STUDENT: "bg-blue-100 text-blue-800 border-blue-200",
  PARENT: "bg-purple-100 text-purple-800 border-purple-200",
  STAFF: "bg-orange-100 text-orange-800 border-orange-200"
};

export default function PermissionsPage() {
  const { data: catalogue = [], isLoading: loadingCatalogue } = usePermissionCatalogue();

  // Load defaults for all roles
  const roleData = ROLES.map((role) => ({ role, hook: useRoleDefaults(role) }));

  const [roleSets, setRoleSets] = useState<Record<Role, Set<string>> | null>(null);
  const [search, setSearch] = useState("");
  const [selectedRole, setSelectedRole] = useState<Role>("ADMIN");

  const updateRoleMut = useUpdateRoleDefaults();

  // Initialize local sets once data ready
  useMemo(() => {
    if (loadingCatalogue) return;
    const ready = roleData.every(({ hook }) => hook.isLoading === false && hook.data);
    if (!ready) return;
    const initial: Record<Role, Set<string>> = ROLES.reduce((acc, r) => {
      const found = roleData.find((x) => x.role === r)?.hook.data || [];
      acc[r] = new Set(found);
      return acc;
    }, {} as Record<Role, Set<string>>);
    setRoleSets((prev) => prev ?? initial);
  }, [loadingCatalogue, ...roleData.map(({ hook }) => hook.isLoading), ...roleData.map(({ hook }) => (hook.data || []).join(","))]);

  // Group permissions that exist in the catalogue
  const availableGroups = useMemo(() => {
    const groups: Record<string, { icon: any; color: string; permissions: string[] }> = {};
    
    Object.entries(PERMISSION_GROUPS).forEach(([groupName, groupData]) => {
      const existingPerms = groupData.permissions.filter(perm => catalogue.includes(perm));
      if (existingPerms.length > 0) {
        groups[groupName] = {
          ...groupData,
          permissions: existingPerms
        };
      }
    });

    // Add ungrouped permissions
    const groupedPerms = Object.values(groups).flatMap(g => g.permissions);
    const ungroupedPerms = catalogue.filter(perm => !groupedPerms.includes(perm));
    
    if (ungroupedPerms.length > 0) {
      groups["Other Permissions"] = {
        icon: Shield,
        color: "bg-gray-100 text-gray-800",
        permissions: ungroupedPerms
      };
    }

    return groups;
  }, [catalogue]);

  // Filter groups by search
  const filteredGroups = useMemo(() => {
    if (!search.trim()) return availableGroups;
    
    const searchLower = search.toLowerCase();
    const filtered: typeof availableGroups = {};
    
    Object.entries(availableGroups).forEach(([groupName, groupData]) => {
      const matchingPerms = groupData.permissions.filter(perm => 
        perm.toLowerCase().includes(searchLower) || 
        groupName.toLowerCase().includes(searchLower)
      );
      
      if (matchingPerms.length > 0) {
        filtered[groupName] = {
          ...groupData,
          permissions: matchingPerms
        };
      }
    });
    
    return filtered;
  }, [availableGroups, search]);

  const togglePermission = (role: Role, permission: string, enabled: boolean) => {
    if (!roleSets) return;
    
    setRoleSets((prev) => {
      if (!prev) return prev;
      const next: Record<Role, Set<string>> = { ...prev } as Record<Role, Set<string>>;
      const set = new Set(next[role]);
      
      if (enabled) {
        set.add(permission);
      } else {
        set.delete(permission);
      }
      
      next[role] = set;
      return next;
    });
  };

  const toggleGroupForRole = (role: Role, groupPermissions: string[], enable: boolean) => {
    if (!roleSets) return;
    
    setRoleSets((prev) => {
      if (!prev) return prev;
      const next: Record<Role, Set<string>> = { ...prev } as Record<Role, Set<string>>;
      const set = new Set(next[role]);
      
      groupPermissions.forEach(perm => {
        if (enable) {
          set.add(perm);
        } else {
          set.delete(perm);
        }
      });
      
      next[role] = set;
      return next;
    });
  };

  const isGroupFullyEnabled = (role: Role, groupPermissions: string[]) => {
    if (!roleSets) return false;
    return groupPermissions.every(perm => roleSets[role].has(perm));
  };

  const isGroupPartiallyEnabled = (role: Role, groupPermissions: string[]) => {
    if (!roleSets) return false;
    return groupPermissions.some(perm => roleSets[role].has(perm)) && 
           !groupPermissions.every(perm => roleSets[role].has(perm));
  };

  const onSaveRole = async (role: Role) => {
    if (!roleSets) return;
    try {
      await updateRoleMut.mutateAsync({ role, codes: Array.from(roleSets[role]) });
      toast.success(`Permissions saved for ${role}`);
    } catch {
      toast.error(`Failed to save permissions for ${role}`);
    }
  };

  const onResetRole = (role: Role) => {
    const originalData = roleData.find(x => x.role === role)?.hook.data || [];
    if (!roleSets) return;
    
    setRoleSets(prev => {
      if (!prev) return prev;
      return {
        ...prev,
        [role]: new Set(originalData)
      };
    });
    toast.success(`Reset permissions for ${role}`);
  };

  const allLoading = loadingCatalogue || roleSets === null || roleData.some(({ hook }) => hook.isLoading);

  if (allLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading permissions...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Role Permissions</h1>
          <p className="text-gray-600 mt-1">Configure permissions for each user role in your system</p>
        </div>
        
        <div className="flex items-center gap-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
            <Input
              placeholder="Search permissions..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-10 w-72"
            />
          </div>
        </div>
      </div>

      {/* Role Tabs */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Shield className="h-5 w-5" />
            Select Role to Configure
          </CardTitle>
          <CardDescription>
            Choose a role to view and modify its permissions
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-2">
            {ROLES.map((role) => (
              <button
                key={role}
                onClick={() => setSelectedRole(role)}
                className={`px-4 py-2 rounded-lg border transition-all duration-200 ${
                  selectedRole === role
                    ? ROLE_COLORS[role] + " shadow-md"
                    : "bg-gray-50 text-gray-700 border-gray-200 hover:bg-gray-100"
                }`}
              >
                <div className="flex items-center gap-2">
                  <Users className="h-4 w-4" />
                  <span className="font-medium">{role}</span>
                  {roleSets && (
                    <Badge variant="secondary" className="text-xs">
                      {roleSets[role].size}
                    </Badge>
                  )}
                </div>
              </button>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Permissions Configuration */}
      <Card>
        <CardHeader className="border-b">
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <span className={`px-3 py-1 rounded-lg text-sm font-medium ${ROLE_COLORS[selectedRole]}`}>
                  {selectedRole}
                </span>
                Permissions
              </CardTitle>
              <CardDescription>
                Toggle permissions for the {selectedRole.toLowerCase()} role
              </CardDescription>
            </div>
            
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => onResetRole(selectedRole)}
                className="flex items-center gap-2"
              >
                <RotateCcw className="h-4 w-4" />
                Reset
              </Button>
              <Button
                onClick={() => onSaveRole(selectedRole)}
                disabled={updateRoleMut.isPending}
                className="flex items-center gap-2"
              >
                <Save className="h-4 w-4" />
                Save Changes
              </Button>
            </div>
          </div>
        </CardHeader>
        
        <CardContent className="p-6">
          <div className="space-y-6">
            {Object.entries(filteredGroups).map(([groupName, groupData]) => {
              const isFullyEnabled = isGroupFullyEnabled(selectedRole, groupData.permissions);
              const isPartiallyEnabled = isGroupPartiallyEnabled(selectedRole, groupData.permissions);
              const IconComponent = groupData.icon;

              return (
                <div key={groupName} className="space-y-4">
                  {/* Group Header */}
                  <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                    <div className="flex items-center gap-3">
                      <div className={`p-2 rounded-lg ${groupData.color}`}>
                        <IconComponent className="h-5 w-5" />
                      </div>
                      <div>
                        <h3 className="font-semibold text-gray-900">{groupName}</h3>
                        <p className="text-sm text-gray-600">
                          {groupData.permissions.length} permission{groupData.permissions.length !== 1 ? 's' : ''}
                        </p>
                      </div>
                    </div>
                    
                    <div className="flex items-center gap-3">
                      <Label 
                        htmlFor={`group-${groupName}-${selectedRole}`}
                        className="text-sm font-medium"
                      >
                        {isFullyEnabled ? 'All enabled' : isPartiallyEnabled ? 'Partially enabled' : 'All disabled'}
                      </Label>
                      <Switch
                        id={`group-${groupName}-${selectedRole}`}
                        checked={isFullyEnabled}
                        onCheckedChange={(checked) => 
                          toggleGroupForRole(selectedRole, groupData.permissions, checked)
                        }
                        className={isPartiallyEnabled ? 'opacity-70' : ''}
                      />
                    </div>
                  </div>

                  {/* Individual Permissions */}
                  <div className="grid gap-3 ml-4 pl-4 border-l-2 border-gray-200">
                    {groupData.permissions.map((permission) => (
                      <div key={permission} className="flex items-center justify-between p-3 bg-white border border-gray-200 rounded-lg hover:border-gray-300 transition-colors">
                        <div className="flex-1">
                          <div className="font-medium text-gray-900">{permission}</div>
                          <div className="text-sm text-gray-500">
                            {permission.split('_').map(word => 
                              word.charAt(0) + word.slice(1).toLowerCase()
                            ).join(' ')} permission
                          </div>
                        </div>
                        
                        <Switch
                          checked={roleSets ? roleSets[selectedRole].has(permission) : false}
                          onCheckedChange={(checked) => 
                            togglePermission(selectedRole, permission, checked)
                          }
                        />
                      </div>
                    ))}
                  </div>

                  {Object.keys(filteredGroups).indexOf(groupName) < Object.keys(filteredGroups).length - 1 && (
                    <Separator className="my-6" />
                  )}
                </div>
              );
            })}
          </div>

          {Object.keys(filteredGroups).length === 0 && (
            <div className="text-center py-12">
              <Shield className="mx-auto h-12 w-12 text-gray-400 mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">No permissions found</h3>
              <p className="text-gray-600">
                {search ? 'Try adjusting your search terms.' : 'No permissions available to configure.'}
              </p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}