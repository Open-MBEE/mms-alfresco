package org.openmbee.mms.security.config;

import org.openmbee.mms.security.JwtAuthenticationEntryPoint;
import org.openmbee.mms.security.JwtAuthenticationTokenFilter;
import org.openmbee.mms.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpMethod.*;

@Configuration
@PropertySource("classpath:application.properties")
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableTransactionManagement
class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${ldap.provider.url}")
    private String providerUrl;

    @Value("${ldap.provider.userdn}")
    private String providerUserDn;

    @Value("${ldap.provider.password}")
    private String providerPassword;

    @Value("${ldap.user.dn.patterns}")
    private String userDnPatterns;

    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @Autowired
    private UserDetailsServiceImpl userDetailsServiceImpl;

    private static final String[] AUTH_WHITELIST = {
        // -- swagger ui
        "/swagger-resources/**", "/swagger-ui.html", "/v2/api-docs", "/webjars/**"};

    public SecurityConfig() {
        /*
         * Ignores the default configuration, useless in our case (session management, etc..)
         */
        super(true);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable().exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and().authorizeRequests()
            .antMatchers(HttpMethod.OPTIONS, "/**").permitAll().antMatchers("/authentication/**").permitAll()
            .antMatchers("/webjars/**").permitAll().antMatchers("/images/**").permitAll()
            .antMatchers("/swagger-resources/**").permitAll().antMatchers("/v2/api-docs/**").permitAll()
            .antMatchers("/swagger-ui.html").permitAll().anyRequest().authenticated();

        http.addFilterBefore(jwtAuthenticationTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(corsFilter(), ExceptionTranslationFilter.class);
        http.headers().cacheControl();
    }

    @Bean
    LdapAuthoritiesPopulator ldapAuthoritiesPopulator() throws Exception {

        /*
          Specificity here : we don't get the Role by reading the members of available groups (which is implemented by
          default in Spring security LDAP), but we retrieve the groups from the field memberOf of the user.
         */
        class MyLdapAuthoritiesPopulator implements LdapAuthoritiesPopulator {

            SpringSecurityLdapTemplate ldapTemplate;
            public final String[] GROUP_ATTRIBUTE = {"cn"};
            public final String GROUP_MEMBER_OF = "memberof";

            MyLdapAuthoritiesPopulator(ContextSource contextSource) {
                ldapTemplate = new SpringSecurityLdapTemplate(contextSource);
            }

            @Override
            public Collection<? extends GrantedAuthority> getGrantedAuthorities(DirContextOperations userData,
                String username) {

                String[] groupDns = userData.getStringAttributes(GROUP_MEMBER_OF);

                String roles = Stream.of(groupDns).map(groupDn -> {
                    LdapName groupLdapName = (LdapName) ldapTemplate.retrieveEntry(groupDn, GROUP_ATTRIBUTE).getDn();
                    // split DN in its different components et get only the last one (cn=my_group)
                    // getValue() allows to only get get the value of the pair (cn=>my_group)
                    return groupLdapName.getRdns().stream().map(Rdn::getValue).reduce((a, b) -> b).orElse(null);
                }).map(x -> (String) x).collect(Collectors.joining(","));

                return AuthorityUtils.commaSeparatedStringToAuthorityList(roles);
            }
        }

        return new MyLdapAuthoritiesPopulator(contextSource());
    }

    /**
     * Configure AuthenticationManagerBuilder to use the specified DetailsService.
     *
     * @param auth the {@link AuthenticationManagerBuilder} à utiliser
     * @throws Exception
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        /*
            see this article : https://spring.io/guides/gs/authenticating-ldap/
            We  redefine our own LdapAuthoritiesPopulator which need ContextSource().
            We need to delegate the creation of the contextSource out of the builder-configuration.
        */
        auth.ldapAuthentication().userDnPatterns(userDnPatterns).ldapAuthoritiesPopulator(ldapAuthoritiesPopulator())
            .contextSource(contextSource());

    }

    @Bean
    public BaseLdapPathContextSource contextSource() throws Exception {
        DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(providerUrl);
        contextSource.setUserDn(providerUserDn);
        contextSource.setPassword(providerPassword);
        return contextSource;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        /*
          Overloaded to expose Authenticationmanager's bean created by configure(AuthenticationManagerBuilder).
           This bean is used by the AuthenticationController.
         */
        return super.authenticationManagerBean();
    }

    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter() throws Exception {
        JwtAuthenticationTokenFilter authenticationTokenFilter = new JwtAuthenticationTokenFilter();
        authenticationTokenFilter.setAuthenticationManager(authenticationManagerBean());
        return authenticationTokenFilter;
    }

    private CorsFilter corsFilter() {
        /*
         CORS requests are managed only if headers Origin and Access-Control-Request-Method are available on OPTIONS requests
         (this filter is simply ignored in other cases).
         This filter can be used as a replacement for the @Cors annotation.
        */
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("*");
        config.addAllowedHeader(ORIGIN);
        config.addAllowedHeader(CONTENT_TYPE);
        config.addAllowedHeader(ACCEPT);
        config.addAllowedHeader(AUTHORIZATION);
        config.addAllowedMethod(GET);
        config.addAllowedMethod(PUT);
        config.addAllowedMethod(POST);
        config.addAllowedMethod(OPTIONS);
        config.addAllowedMethod(DELETE);
        config.addAllowedMethod(PATCH);
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

}