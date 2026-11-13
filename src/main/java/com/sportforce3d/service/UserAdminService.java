package com.sportforce3d.service;

import com.sportforce3d.model.Role;
import com.sportforce3d.model.User;
import com.sportforce3d.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class UserAdminService {

    private static final Logger log = LoggerFactory.getLogger(UserAdminService.class);

    private final UserRepository users;
    private final Session session;

    public UserAdminService(UserRepository users, Session session) {
        this.users = users;
        this.session = session;
    }

    public List<User> listUsers() {
        session.requireRole(Role.ADMIN);
        return users.findAll();
    }

    public User setActive(long userId, boolean active) {
        User admin = session.requireRole(Role.ADMIN);
        User target = find(userId);
        if (!active && target.id() == admin.id()) {
            throw new ServiceException("No puedes desactivar tu propia cuenta.");
        }
        if (!active && target.isAdmin() && target.active() && users.countActiveWithRole(Role.ADMIN) <= 1) {
            throw new ServiceException("Debe quedar al menos un administrador activo.");
        }
        users.setActive(userId, active);
        log.info("ADMIN {} cambió active={} del usuario {}", admin.id(), active, userId);
        return find(userId);
    }

    public User setAdmin(long userId, boolean admin) {
        User me = session.requireRole(Role.ADMIN);
        User target = find(userId);
        if (target.isAdmin() == admin) {
            return target;
        }
        if (!admin && target.id() == me.id()) {
            throw new ServiceException("No puedes quitarte tu propio rol de administrador.");
        }
        if (!admin && target.active() && users.countActiveWithRole(Role.ADMIN) <= 1) {
            throw new ServiceException("Debe quedar al menos un administrador activo.");
        }
        Set<Role> roles = target.roles().isEmpty() ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(target.roles());
        if (admin) {
            roles.add(Role.ADMIN);
        } else {
            roles.remove(Role.ADMIN);
            roles.add(Role.CLIENTE);
        }
        users.replaceRoles(userId, roles);
        log.info("ADMIN {} cambió roles del usuario {} a {}", me.id(), userId, roles);
        return find(userId);
    }

    private User find(long userId) {
        return users.findById(userId).orElseThrow(() -> new ServiceException("El usuario ya no existe."));
    }
}
