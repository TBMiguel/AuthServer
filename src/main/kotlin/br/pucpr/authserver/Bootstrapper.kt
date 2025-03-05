package br.pucpr.authserver

import br.pucpr.authserver.roles.Role
import br.pucpr.authserver.roles.RoleRepository
import br.pucpr.authserver.users.User
import br.pucpr.authserver.users.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component

@Component
class Bootstrapper(
    val userRepository: UserRepository,
    val rolesRepository: RoleRepository,
    @Qualifier("defaultAdmin") val adminUser: User
): ApplicationListener<ContextRefreshedEvent> {
    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        val adminRole = rolesRepository.findByName("ADMIN")
            ?: rolesRepository
                .save(Role(name = "ADMIN", description = "System administrator"))
                .also {
                    rolesRepository.save(Role(name = "USER", description = "Premium User"))
                    log.info("ADMIN and USER roles created")
                }

        if (userRepository.findByRole(adminRole.name).isEmpty()) {
            adminUser.roles.add(adminRole)
            userRepository.save(adminUser)
            log.info("Administrator created")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(Bootstrapper::class.java)
    }
}
