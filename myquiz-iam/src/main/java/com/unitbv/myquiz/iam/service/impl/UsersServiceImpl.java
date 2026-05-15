package com.unitbv.myquiz.iam.service.impl;

import com.unitbv.myquiz.iam.entity.Permission;
import com.unitbv.myquiz.iam.entity.Role;
import com.unitbv.myquiz.iam.entity.User;
import com.unitbv.myquiz.iam.exceptions.UserAlreadyExistsException;
import com.unitbv.myquiz.iam.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.unitbv.myquiz.iam.repository.UsersRepository;
import com.unitbv.myquiz.iam.service.UsersService;

@Service
public class UsersServiceImpl implements UsersService {
    private final UsersRepository usersRepository;

    @Autowired
    private RoleRepository roleRepository;

    public UsersServiceImpl(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @Override
    @CacheEvict(value = "users", allEntries = true)
    public User createUser(String username, String email, String password) {
        if (usersRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Username is taken");
        }
        if (usersRepository.findByEmail(email).isPresent()) {
            throw new UserAlreadyExistsException("Email is already registered");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setHashedPassword(password);
        user.setEnabled(false);
        user.setCreatedAt(OffsetDateTime.now());
        return usersRepository.save(user);
    }

    @Override
    @Cacheable(value="users", key="'all'")
    public List<User> getAllUsers(){
        return usersRepository.findAll();
    }

    @Override
    @Cacheable(value="users", key="#id")
    public Optional<User> getUserById(Long id) {
        return usersRepository.findById(id);
    }

    @Override
    @Cacheable(value="users", key="#username")
    public Optional<User> getUserByUsername(String username) {
        return usersRepository.findByUsername(username);
    }

    @Override
    public Optional<User> getByUsernameOrEmail(String identifier) {
        Optional<User> user = usersRepository.findByUsername(identifier);
        if (user.isEmpty()) {
            user = usersRepository.findByEmail(identifier);
        }
        return user;
    }

    @Override
    @CacheEvict(value = "users", allEntries = true)
    public User updateUser(Long id, String username, String email, String password) {
        Optional<User> optionalUser = usersRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setUsername(username);
            user.setEmail(email);
            user.setHashedPassword(password);
            user.setUpdatedAt(OffsetDateTime.now());
            return usersRepository.save(user);
        }
        return null;
    }

    @Override
    @CacheEvict(value = "users", allEntries = true)
    public void deleteUser(Long id) {
        usersRepository.deleteById(id);
    }

    @Override
    @CacheEvict(value = "users", allEntries = true)
    public User assignRoleToUser(Long userId, Long roleId) {
        User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new RuntimeException("Role not found with id: " + roleId));

        user.getRoles().add(role);
        return usersRepository.save(user);
    }

    @Override
    @CacheEvict(value = "users", allEntries = true)
    public User removeRoleFromUser(Long userId, Long roleId) {
        User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new RuntimeException("Role not found with id: " + roleId));

        user.getRoles().remove(role);
        return usersRepository.save(user);
    }

    @Override
    public Set<Role> getUserRoles(Long userId) {
        User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return user.getRoles();
    }

    @Override
    public Set<Permission> getUserPermissions(Long userId) {
        User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Set<Permission> permissions = new HashSet<>();
        for (Role role : user.getRoles()) {
            permissions.addAll(role.getPermissions());
        }
        return permissions;
    }

    @Override
    public boolean hasPermission(Long userId, String permissionName) {
        Set<Permission> permissions = getUserPermissions(userId);
        return permissions.stream()
            .anyMatch(p -> p.getName().equals(permissionName));
    }

    @Override
    @CacheEvict(value = "users", allEntries = true)
    public User enableUser(Long userId) {
        User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        user.setEnabled(true);
        user.setUpdatedAt(OffsetDateTime.now());
        return usersRepository.save(user);
    }

    @Override
    @CacheEvict(value = "users", allEntries = true)
    public User disableUser(Long userId) {
        User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        user.setEnabled(false);
        user.setUpdatedAt(OffsetDateTime.now());
        return usersRepository.save(user);
    }
}
