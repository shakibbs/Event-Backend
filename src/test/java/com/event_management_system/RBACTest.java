package com.event_management_system;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.event_management_system.service.RoleService;
import com.event_management_system.service.UserService;
import com.event_management_system.entity.Event;
import com.event_management_system.entity.Permission;
import com.event_management_system.entity.Role;
import com.event_management_system.entity.RolePermission;
import com.event_management_system.entity.User;
import com.event_management_system.enums.EventVisibility;
import com.event_management_system.repository.EventRepository;
import com.event_management_system.repository.PermissionRepository;
import com.event_management_system.repository.RolePermissionRepository;
import com.event_management_system.repository.RoleRepository;
import com.event_management_system.repository.UserRepository;

@SpringBootTest
@Transactional
@DisplayName("RBAC (Role-Based Access Control) Test Suite")
@SuppressWarnings("null")
public class RBACTest {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private EventRepository eventRepository;

    private User superAdmin;
    private User admin;
    private User attendee;
    private Role superAdminRole;
    private Role adminRole;
    private Role attendeeRole;
    private Event superAdminEvent;
    private Event adminEvent;
    private Event publicEvent;

    @BeforeEach
    public void setUp() {
        // Create permissions and roles (fresh database for each test with create-drop)
        createDefaultPermissions();
        createDefaultRoles();

        // Get roles
        superAdminRole = roleRepository.findByName("SuperAdmin").orElseThrow(() -> 
            new RuntimeException("SuperAdmin role should exist"));
        adminRole = roleRepository.findByName("Admin").orElseThrow(() -> 
            new RuntimeException("Admin role should exist"));
        attendeeRole = roleRepository.findByName("Attendee").orElseThrow(() -> 
            new RuntimeException("Attendee role should exist"));

        // Create test users
        superAdmin = new User();
        superAdmin.setFullName("Test SuperAdmin");
        superAdmin.setEmail("test.superadmin@ems.com");
        superAdmin.setPassword("password");
        superAdmin.setRole(superAdminRole);
        superAdmin.recordCreation("test");
        superAdmin = userRepository.save(superAdmin);

        admin = new User();
        admin.setFullName("Test Admin");
        admin.setEmail("test.admin@ems.com");
        admin.setPassword("password");
        admin.setRole(adminRole);
        admin.recordCreation("test");
        admin = userRepository.save(admin);

        attendee = new User();
        attendee.setFullName("Test Attendee");
        attendee.setEmail("test.attendee@ems.com");
        attendee.setPassword("password");
        attendee.setRole(attendeeRole);
        attendee.recordCreation("test");
        attendee = userRepository.save(attendee);

        // Create test events
        superAdminEvent = new Event();
        superAdminEvent.setTitle("SuperAdmin Event");
        superAdminEvent.setDescription("Event created by SuperAdmin");
        superAdminEvent.setStartTime(LocalDateTime.now().plusDays(1));
        superAdminEvent.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        superAdminEvent.setLocation("SuperAdmin Location");
        superAdminEvent.setVisibility(EventVisibility.PUBLIC);
        superAdminEvent.setOrganizer(superAdmin);
        superAdminEvent.recordCreation("test");
        superAdminEvent = eventRepository.save(superAdminEvent);

        adminEvent = new Event();
        adminEvent.setTitle("Admin Event");
        adminEvent.setDescription("Event created by Admin");
        adminEvent.setStartTime(LocalDateTime.now().plusDays(2));
        adminEvent.setEndTime(LocalDateTime.now().plusDays(2).plusHours(2));
        adminEvent.setLocation("Admin Location");
        adminEvent.setVisibility(EventVisibility.PUBLIC);
        adminEvent.setOrganizer(admin);
        adminEvent.recordCreation("test");
        adminEvent = eventRepository.save(adminEvent);

        publicEvent = new Event();
        publicEvent.setTitle("Public Event");
        publicEvent.setDescription("Public event for all");
        publicEvent.setStartTime(LocalDateTime.now().plusDays(3));
        publicEvent.setEndTime(LocalDateTime.now().plusDays(3).plusHours(2));
        publicEvent.setLocation("Public Location");
        publicEvent.setVisibility(EventVisibility.PUBLIC);
        publicEvent.setOrganizer(attendee);
        publicEvent.recordCreation("test");
        publicEvent = eventRepository.save(publicEvent);
    }

    // ==================== PERMISSION CHECKING TESTS ====================

    @Test
    @DisplayName("Test 1: SuperAdmin has all permissions")
    public void testSuperAdminHasAllPermissions() {
        assertTrue(userService.hasPermission(superAdmin.getId(), "user.manage.all"),
                "SuperAdmin should have user.manage.all permission");
        assertTrue(userService.hasPermission(superAdmin.getId(), "role.manage.all"),
                "SuperAdmin should have role.manage.all permission");
        assertTrue(userService.hasPermission(superAdmin.getId(), "event.manage.all"),
                "SuperAdmin should have event.manage.all permission");
        assertTrue(userService.hasPermission(superAdmin.getId(), "system.config"),
                "SuperAdmin should have system.config permission");
        System.out.println("✓ Test 1 PASSED: SuperAdmin has all permissions");
    }

    @Test
    @DisplayName("Test 2: Admin does NOT have SuperAdmin permissions")
    public void testAdminDoesNotHaveSuperAdminPermissions() {
        assertFalse(userService.hasPermission(admin.getId(), "user.manage.all"),
                "Admin should NOT have user.manage.all permission");
        assertFalse(userService.hasPermission(admin.getId(), "role.manage.all"),
                "Admin should NOT have role.manage.all permission");
        assertFalse(userService.hasPermission(admin.getId(), "event.manage.all"),
                "Admin should NOT have event.manage.all permission");
        System.out.println("✓ Test 2 PASSED: Admin does NOT have SuperAdmin permissions");
    }

    @Test
    @DisplayName("Test 3: Admin has limited permissions")
    public void testAdminHasLimitedPermissions() {
        assertTrue(userService.hasPermission(admin.getId(), "event.manage.own"),
                "Admin should have event.manage.own permission");
        assertTrue(userService.hasPermission(admin.getId(), "event.view.all"),
                "Admin should have event.view.all permission");
        assertTrue(userService.hasPermission(admin.getId(), "event.invite"),
                "Admin should have event.invite permission");
        assertFalse(userService.hasPermission(admin.getId(), "event.manage.all"),
                "Admin should NOT have event.manage.all permission");
        System.out.println("✓ Test 3 PASSED: Admin has limited permissions");
    }

    @Test
    @DisplayName("Test 4: Attendee has only basic permissions")
    public void testAttendeeHasBasicPermissions() {
        assertTrue(userService.hasPermission(attendee.getId(), "event.view.public"),
                "Attendee should have event.view.public permission");
        assertTrue(userService.hasPermission(attendee.getId(), "event.attend"),
                "Attendee should have event.attend permission");
        assertFalse(userService.hasPermission(attendee.getId(), "event.manage.own"),
                "Attendee should NOT have event.manage.own permission");
        assertFalse(userService.hasPermission(attendee.getId(), "event.manage.all"),
                "Attendee should NOT have event.manage.all permission");
        System.out.println("✓ Test 4 PASSED: Attendee has only basic permissions");
    }

    // ==================== EVENT MANAGEMENT TESTS ====================

    @Test
    @DisplayName("Test 5: SuperAdmin can manage any event")
    public void testSuperAdminCanManageAnyEvent() {
        assertTrue(userService.canManageEvent(superAdmin.getId(), adminEvent.getId()),
                "SuperAdmin should be able to manage Admin's event");
        assertTrue(userService.canManageEvent(superAdmin.getId(), publicEvent.getId()),
                "SuperAdmin should be able to manage any public event");
        System.out.println("✓ Test 5 PASSED: SuperAdmin can manage any event");
    }

    @Test
    @DisplayName("Test 6: Admin can manage only their own events")
    public void testAdminCanManageOnlyOwnEvents() {
        assertTrue(userService.canManageEvent(admin.getId(), adminEvent.getId()),
                "Admin should be able to manage their own event");
        assertFalse(userService.canManageEvent(admin.getId(), superAdminEvent.getId()),
                "Admin should NOT be able to manage SuperAdmin's event");
        assertFalse(userService.canManageEvent(admin.getId(), publicEvent.getId()),
                "Admin should NOT be able to manage Attendee's event");
        System.out.println("✓ Test 6 PASSED: Admin can manage only their own events");
    }

    @Test
    @DisplayName("Test 7: Attendee cannot manage any events")
    public void testAttendeeCannotManageEvents() {
        assertFalse(userService.canManageEvent(attendee.getId(), adminEvent.getId()),
                "Attendee should NOT be able to manage Admin's event");
        assertFalse(userService.canManageEvent(attendee.getId(), superAdminEvent.getId()),
                "Attendee should NOT be able to manage SuperAdmin's event");
        assertFalse(userService.canManageEvent(attendee.getId(), publicEvent.getId()),
                "Attendee should NOT be able to manage their own event");
        System.out.println("✓ Test 7 PASSED: Attendee cannot manage any events");
    }

    // ==================== EVENT VIEWING TESTS ====================

    @Test
    @DisplayName("Test 8: SuperAdmin can view all events")
    public void testSuperAdminCanViewAllEvents() {
        assertTrue(userService.canViewEvent(superAdmin.getId(), superAdminEvent.getId()),
                "SuperAdmin should view their own event");
        assertTrue(userService.canViewEvent(superAdmin.getId(), adminEvent.getId()),
                "SuperAdmin should view Admin's event");
        assertTrue(userService.canViewEvent(superAdmin.getId(), publicEvent.getId()),
                "SuperAdmin should view Attendee's event");
        System.out.println("✓ Test 8 PASSED: SuperAdmin can view all events");
    }

    @Test
    @DisplayName("Test 9: Admin can view all events")
    public void testAdminCanViewAllEvents() {
        assertTrue(userService.canViewEvent(admin.getId(), superAdminEvent.getId()),
                "Admin should view SuperAdmin's public event");
        assertTrue(userService.canViewEvent(admin.getId(), adminEvent.getId()),
                "Admin should view their own event");
        assertTrue(userService.canViewEvent(admin.getId(), publicEvent.getId()),
                "Admin should view public events");
        System.out.println("✓ Test 9 PASSED: Admin can view all events");
    }

    @Test
    @DisplayName("Test 10: Attendee can only view public events")
    public void testAttendeeCanOnlyViewPublicEvents() {
        assertTrue(userService.canViewEvent(attendee.getId(), superAdminEvent.getId()),
                "Attendee should view public events");
        assertTrue(userService.canViewEvent(attendee.getId(), publicEvent.getId()),
                "Attendee should view their own public event");
        System.out.println("✓ Test 10 PASSED: Attendee can only view public events");
    }

    // ==================== USER MANAGEMENT TESTS ====================

    @Test
    @DisplayName("Test 11: SuperAdmin can manage all users")
    public void testSuperAdminCanManageAllUsers() {
        assertTrue(userService.canManageUser(superAdmin.getId(), admin.getId()),
                "SuperAdmin should manage Admin user");
        assertTrue(userService.canManageUser(superAdmin.getId(), attendee.getId()),
                "SuperAdmin should manage Attendee user");
        assertTrue(userService.canManageUser(superAdmin.getId(), superAdmin.getId()),
                "SuperAdmin should manage themselves");
        System.out.println("✓ Test 11 PASSED: SuperAdmin can manage all users");
    }

    @Test
    @DisplayName("Test 12: Admin can manage only Attendee users")
    public void testAdminCanManageOnlyAttendees() {
        assertTrue(userService.canManageUser(admin.getId(), attendee.getId()),
                "Admin should manage Attendee user");
        assertFalse(userService.canManageUser(admin.getId(), superAdmin.getId()),
                "Admin should NOT manage SuperAdmin");
        assertTrue(userService.canManageUser(admin.getId(), admin.getId()),
                "Admin should manage themselves");
        System.out.println("✓ Test 12 PASSED: Admin can manage only Attendees");
    }

    @Test
    @DisplayName("Test 13: Attendee can only manage themselves")
    public void testAttendeeCanOnlyManageThemselves() {
        assertTrue(userService.canManageUser(attendee.getId(), attendee.getId()),
                "Attendee should manage themselves");
        assertFalse(userService.canManageUser(attendee.getId(), admin.getId()),
                "Attendee should NOT manage Admin");
        assertFalse(userService.canManageUser(attendee.getId(), superAdmin.getId()),
                "Attendee should NOT manage SuperAdmin");
        System.out.println("✓ Test 13 PASSED: Attendee can only manage themselves");
    }

    // ==================== ROLE AND PERMISSION MANAGEMENT TESTS ====================

    @Test
    @DisplayName("Test 14: Roles have correct permissions assigned")
    public void testRolesHaveCorrectPermissions() {
        assertEquals(4, superAdminRole.getPermissions().size(),
                "SuperAdmin should have 4 permissions");
        assertEquals(4, adminRole.getPermissions().size(),
                "Admin should have 4 permissions");
        assertEquals(3, attendeeRole.getPermissions().size(),
                "Attendee should have 3 permissions");
        System.out.println("✓ Test 14 PASSED: Roles have correct permissions assigned");
    }

    @Test
    @DisplayName("Test 15: SuperAdmin can add permission to role")
    public void testSuperAdminCanAddPermissionToRole() {
        Permission permission = permissionRepository.findByName("event.view.public").orElse(null);
        assertNotNull(permission, "Permission should exist");

        boolean result = roleService.assignPermissionToRole(adminRole.getId(), permission.getId());
        assertTrue(result, "SuperAdmin should be able to add permission to role");

        Role updatedRole = roleRepository.findById(adminRole.getId()).orElse(null);
        assertNotNull(updatedRole, "Role should exist");
        assertTrue(updatedRole.getPermissions().stream()
                .anyMatch(p -> p.getId().equals(permission.getId())),
                "Permission should be added to role");
        System.out.println("✓ Test 15 PASSED: SuperAdmin can add permission to role");
    }

    @Test
    @DisplayName("Test 16: SuperAdmin can remove permission from role")
    public void testSuperAdminCanRemovePermissionFromRole() {
        Permission permission = permissionRepository.findByName("event.manage.own").orElse(null);
        assertNotNull(permission, "Permission should exist");

        boolean result = roleService.removePermissionFromRole(adminRole.getId(), permission.getId());
        assertTrue(result, "SuperAdmin should be able to remove permission from role");

        Role updatedRole = roleRepository.findById(adminRole.getId()).orElse(null);
        assertNotNull(updatedRole, "Role should exist");
        assertFalse(updatedRole.getPermissions().stream()
                .anyMatch(p -> p.getId().equals(permission.getId())),
                "Permission should be removed from role");
        System.out.println("✓ Test 16 PASSED: SuperAdmin can remove permission from role");
    }

    @Test
    @DisplayName("Test 17: RolePermission junction table works correctly")
    public void testRolePermissionJunctionTable() {
        java.util.List<RolePermission> superAdminRolePermissions = 
            rolePermissionRepository.findByRole(superAdminRole);
        
        assertNotNull(superAdminRolePermissions, "RolePermissions should exist");
        assertFalse(superAdminRolePermissions.isEmpty(), "SuperAdmin should have role permissions");
        assertEquals(4, superAdminRolePermissions.size(), "SuperAdmin should have 4 role permissions");
        
        boolean hasPerm = rolePermissionRepository.existsByRoleAndPermission(
            superAdminRole,
            permissionRepository.findByName("user.manage.all").orElse(null)
        );
        assertTrue(hasPerm, "SuperAdmin should have user.manage.all permission");
        System.out.println("✓ Test 17 PASSED: RolePermission junction table works correctly");
    }

    // ==================== INTEGRATION TESTS ====================

    @Test
    @DisplayName("Test 18: User role and permissions are correctly linked")
    public void testUserRoleAndPermissionsLink() {
        User testAdmin = userRepository.findById(admin.getId()).orElse(null);
        assertNotNull(testAdmin, "User should exist");
        assertNotNull(testAdmin.getRole(), "User should have a role");
        assertEquals("Admin", testAdmin.getRole().getName(), "User role should be Admin");
        
        java.util.Set<Permission> permissions = testAdmin.getRole().getPermissions();
        assertNotNull(permissions, "Permissions should exist");
        assertFalse(permissions.isEmpty(), "Admin should have permissions");
        
        boolean hasEventManageOwn = permissions.stream()
            .anyMatch(p -> "event.manage.own".equals(p.getName()));
        assertTrue(hasEventManageOwn, "Admin should have event.manage.own permission");
        System.out.println("✓ Test 18 PASSED: User role and permissions are correctly linked");
    }

    @Test
    @DisplayName("Test 19: Default roles created on startup")
    public void testDefaultRolesCreatedOnStartup() {
        assertNotNull(superAdminRole, "SuperAdmin role should exist");
        assertNotNull(adminRole, "Admin role should exist");
        assertNotNull(attendeeRole, "Attendee role should exist");
        
        assertEquals("SuperAdmin", superAdminRole.getName());
        assertEquals("Admin", adminRole.getName());
        assertEquals("Attendee", attendeeRole.getName());
        System.out.println("✓ Test 19 PASSED: Default roles created on startup");
    }

    @Test
    @DisplayName("Test 20: Default permissions created on startup")
    public void testDefaultPermissionsCreatedOnStartup() {
        java.util.List<Permission> allPermissions = permissionRepository.findAllByDeletedFalse();
        assertNotNull(allPermissions, "Permissions should exist");
        assertEquals(11, allPermissions.size(), "Should have 11 default permissions");
        
        assertTrue(permissionRepository.findByName("user.manage.all").isPresent());
        assertTrue(permissionRepository.findByName("role.manage.all").isPresent());
        assertTrue(permissionRepository.findByName("event.manage.all").isPresent());
        System.out.println("✓ Test 20 PASSED: Default permissions created on startup");
    }

    // Helper methods for test setup
    private void createDefaultPermissions() {
        String[][] permissionData = {
            {"user.manage.all", "Can manage all users in the system"},
            {"role.manage.all", "Can manage all roles in the system"},
            {"event.manage.all", "Can manage all events in the system"},
            {"system.config", "Can configure system settings"},
            {"user.manage.own", "Can manage own users/team"},
            {"event.manage.own", "Can manage own events"},
            {"event.view.all", "Can view all events"},
            {"event.invite", "Can invite users to events"},
            {"event.view.public", "Can view public events"},
            {"event.view.invited", "Can view invited events"},
            {"event.attend", "Can attend events"}
        };
        
        for (String[] data : permissionData) {
            Permission perm = new Permission();
            perm.setName(data[0]);
            perm.setDescription(data[1]);
            perm.recordCreation("test");
            permissionRepository.save(perm);
        }
        permissionRepository.flush();
    }

    private void createDefaultRoles() {
        // Create SuperAdmin role
        createRoleWithPermissions("SuperAdmin", 
            "user.manage.all", "role.manage.all", "event.manage.all", "system.config");

        // Create Admin role
        createRoleWithPermissions("Admin", 
            "user.manage.own", "event.manage.own", "event.view.all", "event.invite");

        // Create Attendee role
        createRoleWithPermissions("Attendee", 
            "event.view.public", "event.view.invited", "event.attend");
    }

    private void createRoleWithPermissions(String roleName, String... permissionNames) {
        Role role = new Role();
        role.setName(roleName);
        role.recordCreation("test");
        Role savedRole = roleRepository.save(role);
        roleRepository.flush();
        
        for (String permissionName : permissionNames) {
            permissionRepository.findByName(permissionName).ifPresent(permission -> {
                RolePermission rolePermission = new RolePermission(savedRole, permission);
                rolePermissionRepository.save(rolePermission);
            });
        }
        rolePermissionRepository.flush();
    }

}
