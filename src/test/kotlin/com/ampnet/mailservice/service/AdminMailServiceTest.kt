package com.ampnet.mailservice.service

import com.ampnet.mailservice.amqp.walletservice.WalletTypeAmqp
import com.ampnet.mailservice.enums.MailType
import com.ampnet.mailservice.service.impl.AdminMailServiceImpl
import com.ampnet.mailservice.service.impl.mail.toMailFormat
import com.ampnet.userservice.proto.UserResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.UUID

class AdminMailServiceTest : MailServiceTestBase() {

    private val service: AdminMailService by lazy {
        AdminMailServiceImpl(
            mailSender, applicationProperties, linkResolverService, headlessCmsService, userService
        )
    }

    @Test
    fun mustSetCorrectWithdrawRequestMail() {
        suppose("User service returns a list of token issuers") {
            val tokenIssuer = UserResponse.newBuilder()
                .setUuid(UUID.randomUUID().toString())
                .setEmail(testContext.tokenIssuerMail)
                .setCoop(coop)
                .build()
            Mockito.`when`(userService.getTokenIssuers(coop))
                .thenReturn(listOf(tokenIssuer))
        }
        suppose("User service returns user response") {
            testContext.user = generateUserResponse("user@mail.com")
            Mockito.`when`(userService.getUsers(listOf(testContext.user.uuid)))
                .thenReturn(listOf(testContext.user))
        }
        suppose("Service send withdraw request mail to user and token issuers") {
            mockHeadlessCmsServiceResponse(MailType.TOKEN_ISSUER_WITHDRAWAL_REQUEST_MAIL)
            service.sendWithdrawRequestMail(UUID.fromString(testContext.user.uuid), testContext.amount)
        }

        verify("The mail is sent to token issuer and has correct data") {
            val mailList = wiser.messages
            assertThat(mailList).hasSize(1)
            val tokenIssuerMail = mailList.first()

            assertThat(tokenIssuerMail.envelopeSender).isEqualTo(applicationProperties.mail.sender)
            assertThat(tokenIssuerMail.envelopeReceiver).isEqualTo(testContext.tokenIssuerMail)
            val manageWithdrawalsLink = applicationProperties.mail.baseUrl + "/" + coop + "/" +
                applicationProperties.mail.manageWithdrawalsPath

            val mailText = tokenIssuerMail.mimeMessage.content.toString()
            assertThat(mailText).contains(testContext.amount.toMailFormat())
            assertThat(mailText).contains(manageWithdrawalsLink)
        }
    }

    @Test
    fun mustSetCorrectSendNewOrganizationWalletMail() {
        suppose("Service sent mail for new organization wallet created") {
            val platformManager = UserResponse.newBuilder()
                .setUuid(UUID.randomUUID().toString())
                .setEmail(testContext.receiverMail)
                .build()
            Mockito.`when`(userService.getPlatformManagers(coop))
                .thenReturn(listOf(platformManager))
            mockHeadlessCmsServiceResponse(MailType.NEW_ORGANIZATION_WALLET_MAIL)
            service.sendNewWalletNotificationMail(WalletTypeAmqp.ORGANIZATION, coop, activationData)
        }

        verify("The mail is sent to right receiver and has correct data") {
            val mailList = wiser.messages
            val userMail = mailList.first()
            assertThat(userMail.envelopeSender).isEqualTo(applicationProperties.mail.sender)
            assertThat(userMail.envelopeReceiver).isEqualTo(testContext.receiverMail)
            val confirmationUserLink = applicationProperties.mail.baseUrl + "/" + coop + "/" +
                applicationProperties.mail.newWalletPath + "/groups"
            val mailText = userMail.mimeMessage.content.toString()
            assertThat(mailText).contains(confirmationUserLink)
            assertThat(mailText).doesNotContain(activationData)
        }
    }

    @Test
    fun mustSetCorrectSendNewWalletMails() {
        suppose("Service sent mails for new user and project wallet created") {
            val platformManager = UserResponse.newBuilder()
                .setUuid(UUID.randomUUID().toString())
                .setEmail(testContext.receiverMail)
                .build()
            Mockito.`when`(userService.getPlatformManagers(coop))
                .thenReturn(listOf(platformManager))
            mockHeadlessCmsServiceResponse(MailType.NEW_USER_WALLET_MAIL)
            mockHeadlessCmsServiceResponse(MailType.NEW_PROJECT_WALLET_MAIL)
            service.sendNewWalletNotificationMail(WalletTypeAmqp.USER, coop, activationData)
            service.sendNewWalletNotificationMail(WalletTypeAmqp.PROJECT, coop, activationData)
        }

        verify("Both mails are sent to right receivers and have confirmation link") {
            val mailList = wiser.messages
            val userMail = mailList.first()
            assertThat(userMail.envelopeSender).isEqualTo(applicationProperties.mail.sender)
            assertThat(userMail.envelopeReceiver).isEqualTo(testContext.receiverMail)
            val confirmationUserLink = applicationProperties.mail.baseUrl + "/" + coop + "/" +
                applicationProperties.mail.newWalletPath + "/user"
            val userMailText = userMail.mimeMessage.content.toString()
            assertThat(userMailText).contains(confirmationUserLink)
            assertThat(userMailText).contains(activationData)

            val projectMail = mailList[1]
            assertThat(projectMail.envelopeSender).isEqualTo(applicationProperties.mail.sender)
            assertThat(projectMail.envelopeReceiver).isEqualTo(testContext.receiverMail)
            val confirmationProjectLink = applicationProperties.mail.baseUrl + "/" + coop + "/" +
                applicationProperties.mail.newWalletPath + "/project"
            val projectMailText = projectMail.mimeMessage.content.toString()
            assertThat(projectMailText).contains(confirmationProjectLink)
            assertThat(projectMailText).doesNotContain(activationData)
        }
    }
}
