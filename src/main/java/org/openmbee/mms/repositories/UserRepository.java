package org.openmbee.mms.repositories;

import java.util.Set;

import org.springframework.stereotype.Repository;

import org.openmbee.mms.domains.User;

@Repository
public interface UserRepository {

    User findByEmail(String email);

    User findByActivation(String activation);

    Set<User> findBySpecialty(Long id);

    Set<User> searchByName(String name);
}