package com.ampnet.mailservice.service.impl

import com.ampnet.mailservice.amqp.blockchainservice.SuccessfullyInvestedMessage
import com.ampnet.mailservice.amqp.projectservice.MailOrgInvitationMessage
import com.ampnet.mailservice.amqp.userservice.MailConfirmationMessage
import com.ampnet.mailservice.amqp.userservice.MailResetPasswordMessage
import com.ampnet.mailservice.amqp.walletservice.WalletTypeAmqp
import com.ampnet.mailservice.config.ApplicationProperties
import com.ampnet.mailservice.enums.MailType
import com.ampnet.mailservice.exception.ResourceNotFoundException
import com.ampnet.mailservice.grpc.blockchainservice.BlockchainService
import com.ampnet.mailservice.grpc.projectservice.ProjectService
import com.ampnet.mailservice.grpc.userservice.UserService
import com.ampnet.mailservice.grpc.walletservice.WalletService
import com.ampnet.mailservice.service.FileService
import com.ampnet.mailservice.service.HeadlessCmsService
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
import com.ampnet.mailservice.service.pojo.Attachment
import com.ampnet.mailservice.service.pojo.SuccessfullyInvestedTemplateData
import com.ampnet.mailservice.service.pojo.TERMS_OF_SERVICE
import com.ampnet.userservice.proto.UserResponse
import com.ampnet.walletservice.proto.WalletResponse
import mu.KLogging
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Suppress("TooManyFunctions", "LongParameterList")
class UserMailServiceImpl(
    mailSender: JavaMailSender,
    applicationProperties: ApplicationProperties,
    linkResolverService: LinkResolverService,
    headlessCmsService: HeadlessCmsService,
    private val userService: UserService,
    private val projectService: ProjectService,
    private val walletService: WalletService,
    private val fileService: FileService,
    private val blockchainService: BlockchainService
) : UserMailService {

    companion object : KLogging()

    private val confirmationMail: ConfirmationMail by lazy {
        ConfirmationMail(linkResolverService, mailSender, applicationProperties, headlessCmsService)
    }
    private val resetPasswordMail: ResetPasswordMail by lazy {
        ResetPasswordMail(linkResolverService, mailSender, applicationProperties, headlessCmsService)
    }
    private val invitationMail: InvitationMail by lazy {
        InvitationMail(linkResolverService, mailSender, applicationProperties, headlessCmsService)
    }
    private val depositRequestMail: DepositRequestMail by lazy {
        DepositRequestMail(linkResolverService, mailSender, applicationProperties, headlessCmsService)
    }
    private val depositMail: DepositInfoMail by lazy {
        DepositInfoMail(
            MailType.DEPOSIT_INFO_MAIL, linkResolverService, mailSender,
            applicationProperties, headlessCmsService
        )
    }
    private val depositInfoFailedMail: DepositInfoMail by lazy {
        DepositInfoMail(
            MailType.DEPOSIT_FAILED_INFO_MAIL, linkResolverService,
            mailSender, applicationProperties, headlessCmsService
        )
    }
    private val depositNoProjectInvestmentMail: DepositInfoMail by lazy {
        DepositInfoMail(
            MailType.DEPOSIT_INFO_NO_PROJECT_TO_INVEST_MAIL, linkResolverService,
            mailSender, applicationProperties, headlessCmsService
        )
    }
    private val withdrawRequestMail: WithdrawRequestMail by lazy {
        WithdrawRequestMail(linkResolverService, mailSender, applicationProperties, headlessCmsService)
    }
    private val withdrawInfoMail: WithdrawInfoMail by lazy {
        WithdrawInfoMail(
            MailType.WITHDRAW_INFO_MAIL, linkResolverService, mailSender,
            applicationProperties, headlessCmsService
        )
    }
    private val withdrawFailedInfoMail: WithdrawInfoMail by lazy {
        WithdrawInfoMail(
            MailType.WITHDRAW_FAILED_INFO_MAIL, linkResolverService,
            mailSender, applicationProperties, headlessCmsService
        )
    }
    private val activatedUserWalletMail: ActivatedUserWalletMail by lazy {
        ActivatedUserWalletMail(linkResolverService, mailSender, applicationProperties, headlessCmsService)
    }
    private val activatedOrganizationWalletMail: ActivatedOrganizationWalletMail by lazy {
        ActivatedOrganizationWalletMail(
            linkResolverService, mailSender, applicationProperties, headlessCmsService
        )
    }
    private val activatedProjectWalletMail: ActivatedProjectWalletMail by lazy {
        ActivatedProjectWalletMail(linkResolverService, mailSender, applicationProperties, headlessCmsService)
    }
    private val failedDeliveryMail: FailedDeliveryMail by lazy {
        FailedDeliveryMail(linkResolverService, mailSender, applicationProperties, headlessCmsService)
    }
    private val projectFullyFundedMail: ProjectFullyFundedMail by lazy {
        ProjectFullyFundedMail(linkResolverService, mailSender, applicationProperties, headlessCmsService)
    }
    private val successfullyInvestedMail: SuccessfullyInvestedMail by lazy {
        SuccessfullyInvestedMail(
            MailType.SUCCESSFULLY_INVESTED_MAIL, linkResolverService,
            mailSender, applicationProperties, headlessCmsService
        )
    }
    private val successfullyInvestedWithoutTosMail: SuccessfullyInvestedMail by lazy {
        SuccessfullyInvestedMail(
            MailType.SUCCESSFULLY_INVESTED_WITHOUT_TOS_MAIL, linkResolverService,
            mailSender, applicationProperties, headlessCmsService
        )
    }

    override fun sendConfirmationMail(request: MailConfirmationMessage) =
        confirmationMail.setTemplateData(request.token, request.coop).setLanguage(request.language)
            .sendTo(request.email)

    override fun sendResetPasswordMail(request: MailResetPasswordMessage) =
        resetPasswordMail.setTemplateData(request.token, request.coop).setLanguage(request.language)
            .sendTo(request.email)

    override fun sendOrganizationInvitationMail(request: MailOrgInvitationMessage) {
        val senderResponse = getUser(request.sender)
        invitationMail.setTemplateData(request.organizationName, request.coop).setLanguage(senderResponse.language)
            .sendTo(request.emails.toList()) { failedMails ->
                val filedMailRecipients = failedMails.map { it.allRecipients.toString() }
                failedDeliveryMail.setTemplateData(filedMailRecipients).setCoop(request.coop)
                    .setLanguage(senderResponse.language).sendTo(senderResponse.email)
            }
    }

    override fun sendDepositRequestMail(user: UUID, amount: Long) {
        val userResponse = getUser(user)
        depositRequestMail.setTemplateData(amount).setCoop(userResponse.coop)
            .setLanguage(userResponse.language).sendTo(userResponse.email)
    }

    override fun sendDepositInfoMail(user: UUID, minted: Boolean) {
        val userResponse = getUser(user)
        val hasProjectWhichCanReceiveInvestment = if (minted.not()) false
        else hasProjectWhichCanReceiveInvestment(userResponse.coop)
        when {
            (minted && hasProjectWhichCanReceiveInvestment) -> depositMail
            (minted && hasProjectWhichCanReceiveInvestment.not()) -> depositNoProjectInvestmentMail
            else -> depositInfoFailedMail
        }.setTemplateData(userResponse.coop)
            .setLanguage(userResponse.language).sendTo(userResponse.email)
    }

    override fun sendWithdrawRequestMail(user: UUID, amount: Long) {
        val userResponse = getUser(user)
        withdrawRequestMail.setTemplateData(amount).setCoop(userResponse.coop)
            .setLanguage(userResponse.language).sendTo(userResponse.email)
    }

    override fun sendWithdrawInfoMail(user: UUID, burned: Boolean) {
        val userResponse = getUser(user)
        val mail = if (burned) withdrawInfoMail.setCoop(userResponse.coop)
        else withdrawFailedInfoMail
        mail.setCoop(userResponse.coop).setLanguage(userResponse.language).sendTo(userResponse.email)
    }

    override fun sendWalletActivatedMail(walletOwner: UUID, walletType: WalletTypeAmqp, activationData: String) {
        val (mail: AbstractMail, user: UserResponse) = when (walletType) {
            WalletTypeAmqp.USER -> {
                val user = getUser(walletOwner)
                Pair(
                    activatedUserWalletMail.setTemplateData(activationData, user.coop).setLanguage(user.language),
                    user
                )
            }
            WalletTypeAmqp.ORGANIZATION -> {
                val organization = projectService.getOrganization(walletOwner)
                val user = getUser(organization.createdByUser)
                Pair(
                    activatedOrganizationWalletMail.setTemplateData(organization, user.coop).setLanguage(user.language),
                    user
                )
            }
            WalletTypeAmqp.PROJECT -> {
                val project = projectService.getProject(walletOwner)
                val user = getUser(project.createdByUser)
                Pair(activatedProjectWalletMail.setTemplateData(project, user.coop).setLanguage(user.language), user)
            }
        }
        mail.sendTo(user.email)
    }

    override fun sendProjectFullyFundedMail(walletHash: String) {
        val wallet = walletService.getWalletByHash(walletHash)
        val project = projectService.getProject(UUID.fromString(wallet.owner))
        val user = getUser(project.createdByUser)
        projectFullyFundedMail.setTemplateData(user, project).setLanguage(user.language).sendTo(user.email)
    }

    override fun sendSuccessfullyInvested(request: SuccessfullyInvestedMessage) {
        val wallets = walletService.getWalletsByHash(
            setOf(
                request.userWalletTxHash, request.projectWalletTxHash
            )
        )
        val user = userService.getUserWithInfo(getOwnerByHash(wallets, request.userWalletTxHash))
        val project = projectService.getProjectWithData(
            UUID.fromString(getOwnerByHash(wallets, request.projectWalletTxHash))
        )
        logger.debug("${project.project.uuid} has terms of service: ${project.tosUrl}")
        val termsOfService = if (project.tosUrl.isNotBlank()) {
            logger.debug("There should be an attachment ${project.tosUrl}")
            Attachment(TERMS_OF_SERVICE, fileService.getTermsOfService(project.tosUrl))
        } else {
            logger.debug("There is no attachment ${project.tosUrl}")
            null
        }
        val mail = if (termsOfService == null) successfullyInvestedWithoutTosMail
        else successfullyInvestedMail
        mail.setTemplateData(
            SuccessfullyInvestedTemplateData(project, request.amount.toLong(), user.coop, termsOfService)
        )
            .setLanguage(user.user.language)
            .sendTo(user.user.email)
    }

    private fun getOwnerByHash(wallets: List<WalletResponse>, hash: String): String =
        wallets.firstOrNull { it.hash == hash }?.owner
            ?: throw ResourceNotFoundException("Missing owner for wallet hash: $hash")

    private fun getUser(userUuid: UUID): UserResponse = getUser(userUuid.toString())

    private fun getUser(userUuid: String): UserResponse =
        userService.getUsers(listOf(userUuid)).firstOrNull()
            ?: throw ResourceNotFoundException("Missing user: $userUuid")

    private fun hasProjectWhichCanReceiveInvestment(coop: String): Boolean {
        val projects = projectService.getActiveProjects(coop)
            .associateBy { UUID.fromString(it.uuid) }
        val wallets = walletService.getWalletsByOwner(projects.keys.toList())
        wallets.forEach { wallet ->
            val projectBalance = blockchainService.getBalance(wallet.hash)
            val expectedFunding = projects[UUID.fromString(wallet.owner)]?.expectedFunding
            if (projectBalance.isLessThan(expectedFunding)) return true
        }
        return false
    }
}

fun Long?.isLessThan(other: Long?) =
    this != null && other != null && this < other
