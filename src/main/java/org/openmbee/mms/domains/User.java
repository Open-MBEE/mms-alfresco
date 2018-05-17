package org.openmbee.mms.domains;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class User extends Base {

    private String email;
    private String firstName;
    private String lastName;
    private String salutation;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @JsonIgnore
    private String activation;

    private boolean verified;
    private boolean enabled;
    private boolean admin;

    private SecurityRole[] roles;

    public User() {
    }

    public User(String email, String password, String firstName, String lastName) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public User(String email, String password, String firstName, String lastName, SecurityRole... roles) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.roles = roles;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void encodePassword(String password) {
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        this.password = bcrypt.encode(password);
    }

    public void updatePassword(String old, String newPass1, String newPass2) {
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        if (!password.equals(bcrypt.encode(old))) {
            throw new IllegalArgumentException("Existing Password invalid");
        }
        if (!newPass1.equals(newPass2)) {
            throw new IllegalArgumentException("New Passwords don't match");
        }
        this.password = bcrypt.encode(newPass1);
    }

    public String getActivation() {
        return activation;
    }

    public void setActivation(String activation) {
        this.activation = activation;
    }

    public SecurityRole[] getRoles() {
        return roles;
    }

    public void setRoles(List<SecurityRole> roles) {
        this.roles = roles.toArray(new SecurityRole[roles.size()]);
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getSalutation() {
        return salutation;
    }

    public void setSalutation(String salutation) {
        this.salutation = salutation;
    }

    public boolean getVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean getAdmin() {
        if (admin) {
            List<SecurityRole> securityRolesList = new ArrayList<>();
            securityRolesList.add(SecurityRole.ROLE_ADMIN);
            setRoles(securityRolesList);
        }
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public enum SecurityRole implements GrantedAuthority {
        ROLE_USER, ROLE_ADMIN;

        @Override
        public String getAuthority() {
            return name();
        }
    }

}