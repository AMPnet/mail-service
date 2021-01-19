package com.ampnet.mailservice.service.impl

import com.ampnet.mailservice.config.ApplicationProperties
import com.ampnet.mailservice.enums.WalletType
import com.ampnet.mailservice.exception.ResourceNotFoundException
import com.ampnet.mailservice.grpc.projectservice.ProjectService
import com.ampnet.mailservice.grpc.userservice.UserService
import com.ampnet.mailservice.grpc.walletservice.WalletService
import com.ampnet.mailservice.proto.MailConfirmationRequest
import com.ampnet.mailservice.proto.OrganizationInvitationRequest
import com.ampnet.mailservice.proto.ResetPasswordRequest
import com.ampnet.mailservice.proto.SuccessfullyInvestedRequest
import com.ampnet.mailservice.service.LinkResolverService
import com.ampnet.mailservice.service.UserMailService
import com.ampnet.mailservice.service.impl.mail.AbstractMail
import com.ampnet.mailservice.service.impl.mail.ActivatedOrganizationWalletMail
import com.ampnet.mailservice.service.impl.mail.ActivatedProjectWalletMail
import com.ampnet.mailservice.service.impl.mail.ActivatedUserWalletMail
import com.ampnet.mailservice.service.impl.mail.ConfirmationMail
import com.ampnet.mailservice.service.impl.mail.DepositInfoMail
import com.ampnet.mailservice.service.impl.mail.DepositRequestMail
import com.ampnet.mailservice.service.impl.mail.FailedDeliveryMail
import com.ampnet.mailservice.service.impl.mail.InvitationMail
import com.ampnet.mailservice.service.impl.mail.ProjectFullyFundedMail
import com.ampnet.mailservice.service.impl.mail.ResetPasswordMail
import com.ampnet.mailservice.service.impl.mail.SuccessfullyInvestedMail
import com.ampnet.mailservice.service.impl.mail.WithdrawInfoMail
import com.ampnet.mailservice.service.impl.mail.WithdrawRequestMail
import com.ampnet.userservice.proto.UserResponse
import com.ampnet.walletservice.proto.WalletResponse
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Suppress("TooManyFunctions")
class UserMailServiceImpl(
    mailSender: JavaMailSender,
    applicationProperties: ApplicationProperties,
    linkResolverService: LinkResolverService,
    private val userService: UserService,
    private val projectService: ProjectService,
    private val walletService: WalletService
) : UserMailService {

    private val confirmationMail: ConfirmationMail by lazy {
        ConfirmationMail(mailSender, applicationProperties, linkResolverService)
    }
    private val resetPasswordMail: ResetPasswordMail by lazy {
        ResetPasswordMail(mailSender, applicationProperties, linkResolverService)
    }
    private val invitationMail: InvitationMail by lazy {
        InvitationMail(mailSender, applicationProperties, linkResolverService)
    }
    private val depositRequestMail: DepositRequestMail by lazy {
        DepositRequestMail(mailSender, applicationProperties, linkResolverService)
    }
    private val depositMail: DepositInfoMail by lazy {
        DepositInfoMail(mailSender, applicationProperties, linkResolverService)
    }
    private val withdrawRequestMail: WithdrawRequestMail by lazy {
        WithdrawRequestMail(mailSender, applicationProperties, linkResolverService)
    }
    private val withdrawInfoMail: WithdrawInfoMail by lazy {
        WithdrawInfoMail(mailSender, applicationProperties, linkResolverService)
    }
    private val activatedUserWalletMail: ActivatedUserWalletMail by lazy {
        ActivatedUserWalletMail(mailSender, applicationProperties, linkResolverService)
    }
    private val activatedOrganizationWalletMail: ActivatedOrganizationWalletMail by lazy {
        ActivatedOrganizationWalletMail(mailSender, applicationProperties, linkResolverService)
    }
    private val activatedProjectWalletMail: ActivatedProjectWalletMail by lazy {
        ActivatedProjectWalletMail(mailSender, applicationProperties, linkResolverService)
    }
    private val failedDeliveryMail: FailedDeliveryMail by lazy {
        FailedDeliveryMail(mailSender, applicationProperties, linkResolverService)
    }
    private val projectFullyFundedMail: ProjectFullyFundedMail by lazy {
        ProjectFullyFundedMail(mailSender, applicationProperties, linkResolverService)
    }
    private val successfullyInvestedMail: SuccessfullyInvestedMail by lazy {
        SuccessfullyInvestedMail(mailSender, applicationProperties, linkResolverService)
    }

    override fun sendConfirmationMail(request: MailConfirmationRequest) =
        confirmationMail.setData(request.token, request.coop).sendTo(request.email, request.language)

    override fun sendResetPasswordMail(request: ResetPasswordRequest) =
        resetPasswordMail.setData(request.token, request.coop).sendTo(request.email, request.language)

    override fun sendOrganizationInvitationMail(request: OrganizationInvitationRequest) =
        invitationMail.setData(request.organization, request.coop)
            .sendTo(request.emailsList.toList(), request.language) { failedMails ->
                val filedMailRecipients = failedMails.map { it.allRecipients.toString() }
                failedDeliveryMail.setData(filedMailRecipients).sendTo(request.senderEmail, request.language)
            }

    override fun sendDepositRequestMail(user: UserResponse, amount: Long) =
        depositRequestMail.setData(amount).sendTo(user.email, user.language)

    override fun sendDepositInfoMail(user: UserResponse, minted: Boolean) =
        depositMail.setData(minted).sendTo(user.email, user.language)

    override fun sendWithdrawRequestMail(user: UserResponse, amount: Long) =
        withdrawRequestMail.setData(amount).sendTo(user.email, user.language)

    override fun sendWithdrawInfoMail(user: UserResponse, burned: Boolean) =
        withdrawInfoMail.setData(burned).sendTo(user.email, user.language)

    override fun sendWalletActivatedMail(walletOwner: String, walletType: WalletType, activationData: String) {
        val (mail: AbstractMail, user: UserResponse) = when (walletType) {
            WalletType.USER -> {
                val user = getUser(walletOwner)
                Pair(activatedUserWalletMail.setData(activationData, user.coop), user)
            }
            WalletType.ORGANIZATION -> {
                val organization = projectService.getOrganization(UUID.fromString(walletOwner))
                val user = getUser(organization.createdByUser)
                Pair(activatedOrganizationWalletMail.setData(organization, user.coop), user)
            }
            WalletType.PROJECT -> {
                val project = projectService.getProject(UUID.fromString(walletOwner))
                val user = getUser(project.createdByUser)
                Pair(activatedProjectWalletMail.setData(project, user.coop), user)
            }
        }
        mail.sendTo(user.email, user.language)
    }

    override fun sendProjectFullyFundedMail(walletHash: String) {
        val wallet = walletService.getWalletByHash(walletHash)
        val project = projectService.getProject(UUID.fromString(wallet.owner))
        val user = getUser(project.createdByUser)
        projectFullyFundedMail.setData(user, project).sendTo(user.email, user.language)
    }

    override fun sendSuccessfullyInvested(request: SuccessfullyInvestedRequest) {
        val wallets = walletService.getWalletsByHash(setOf(request.walletHashFrom, request.walletHashTo))
        val user = getUser(getOwnerByHash(wallets, request.walletHashFrom))
        val project = projectService.getProjectWithData(UUID.fromString(getOwnerByHash(wallets, request.walletHashTo)))
        successfullyInvestedMail.setData(project, request.amount.toLong())
            .sendTo(user.email, user.language)
    }

    private fun getOwnerByHash(wallets: List<WalletResponse>, hash: String): String =
        wallets.firstOrNull { it.hash == hash }?.owner
            ?: throw ResourceNotFoundException("Missing owner for wallet hash: $hash")

    private fun getUser(userUuid: String): UserResponse =
        userService.getUsers(listOf(userUuid)).firstOrNull()
            ?: throw ResourceNotFoundException("Missing user: $userUuid")
}
