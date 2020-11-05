package com.ampnet.mailservice.service.impl

import com.ampnet.mailservice.config.ApplicationProperties
import com.ampnet.mailservice.enums.WalletType
import com.ampnet.mailservice.grpc.userservice.UserService
import com.ampnet.mailservice.service.AdminMailService
import com.ampnet.mailservice.service.LinkResolverService
import com.ampnet.mailservice.service.impl.mail.NewWalletMail
import com.ampnet.mailservice.service.impl.mail.WithdrawTokenIssuerMail
import com.ampnet.userservice.proto.UserResponse
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class AdminMailServiceImpl(
    mailSender: JavaMailSender,
    applicationProperties: ApplicationProperties,
    linkResolverService: LinkResolverService,
    private val userService: UserService
) : AdminMailService {

    private val withdrawTokenIssuerMail: WithdrawTokenIssuerMail by lazy {
        WithdrawTokenIssuerMail(mailSender, applicationProperties, linkResolverService)
    }
    private val newUserWalletMail: NewWalletMail by lazy {
        NewWalletMail(WalletType.USER, mailSender, applicationProperties, linkResolverService)
    }
    private val newOrganizationWalletMail: NewWalletMail by lazy {
        NewWalletMail(WalletType.ORGANIZATION, mailSender, applicationProperties, linkResolverService)
    }
    private val newProjectWalletMail: NewWalletMail by lazy {
        NewWalletMail(WalletType.PROJECT, mailSender, applicationProperties, linkResolverService)
    }

    override fun sendWithdrawRequestMail(user: UserResponse, amount: Long) =
        withdrawTokenIssuerMail.setData(user, amount)
            .sendTo(userService.getTokenIssuers(user.coop).map { it.email })

    override fun sendNewWalletNotificationMail(walletType: WalletType, coop: String, activationData: String) =
        when (walletType) {
            WalletType.USER -> newUserWalletMail
            WalletType.PROJECT -> newProjectWalletMail
            WalletType.ORGANIZATION -> newOrganizationWalletMail
        }.setData(activationData).sendTo(userService.getPlatformManagers(coop).map { it.email })
}
