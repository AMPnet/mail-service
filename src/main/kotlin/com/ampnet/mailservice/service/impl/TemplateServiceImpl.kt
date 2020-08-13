package com.ampnet.mailservice.service.impl

import com.ampnet.mailservice.enums.WalletType
import com.ampnet.mailservice.service.TemplateService
import com.ampnet.mailservice.service.pojo.AmountData
import com.ampnet.mailservice.service.pojo.DepositInfo
import com.ampnet.mailservice.service.pojo.InvitationData
import com.ampnet.mailservice.service.pojo.MailConfirmationData
import com.ampnet.mailservice.service.pojo.NewWalletData
import com.ampnet.mailservice.service.pojo.ResetPasswordData
import com.ampnet.mailservice.service.pojo.UserData
import com.ampnet.mailservice.service.pojo.WithdrawInfo
import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.Mustache
import org.springframework.stereotype.Service
import java.io.StringWriter

@Service
class TemplateServiceImpl : TemplateService {

    private val mustacheFactory = DefaultMustacheFactory()
    private val mailConfirmationTemplate: Mustache by lazy {
        mustacheFactory.compile("mustache/mail-confirmation-template.mustache")
    }
    private val invitationTemplate: Mustache by lazy {
        mustacheFactory.compile("mustache/invitation-template.mustache")
    }
    private val depositRequestTemplate: Mustache by lazy {
        mustacheFactory.compile("mustache/deposit-request-template.mustache")
    }
    private val depositTemplate: Mustache by lazy {
        mustacheFactory.compile("mustache/deposit-template.mustache")
    }
    private val withdrawRequestTemplate: Mustache by lazy {
        mustacheFactory.compile("mustache/withdraw-request-template.mustache")
    }
    private val withdrawTemplate: Mustache by lazy {
        mustacheFactory.compile("mustache/withdraw-template.mustache")
    }
    private val forgotPasswordTemplate: Mustache by lazy {
        mustacheFactory.compile("mustache/forgot-password-template.mustache")
    }
    private val userWalletTemplate: Mustache by lazy {
        mustacheFactory.compile("mustache/user-wallet-template.mustache")
    }
    private val projectWalletTemplate: Mustache by lazy {
        mustacheFactory.compile("mustache/project-wallet-template.mustache")
    }
    private val tokenIssuerWithdrawRequestTemplate: Mustache by lazy {
        mustacheFactory.compile("mustache/token-issuer-withdrawal-template.mustache")
    }

    override fun generateTextForMailConfirmation(data: MailConfirmationData): String {
        return fillTemplate(mailConfirmationTemplate, data)
    }

    override fun generateTextForResetPassword(data: ResetPasswordData): String {
        return fillTemplate(forgotPasswordTemplate, data)
    }

    override fun generateTextForInvitation(data: InvitationData): String {
        return fillTemplate(invitationTemplate, data)
    }

    override fun generateTextForDepositRequest(data: AmountData): String {
        return fillTemplate(depositRequestTemplate, data)
    }

    override fun generateTextForDepositInfo(data: DepositInfo): String {
        return fillTemplate(depositTemplate, data)
    }

    override fun generateTextForWithdrawRequest(data: AmountData): String {
        return fillTemplate(withdrawRequestTemplate, data)
    }

    override fun generateTextForWithdrawInfo(data: WithdrawInfo): String {
        return fillTemplate(withdrawTemplate, data)
    }

    override fun generateTextForNewWallet(data: NewWalletData, walletType: WalletType): String {
        return when (walletType) {
            WalletType.USER -> fillTemplate(userWalletTemplate, NewWalletData("${data.link}/user"))
            WalletType.PROJECT -> fillTemplate(projectWalletTemplate, NewWalletData("${data.link}/project"))
        }
    }

    override fun generateTextForTokenIssuerWithdrawRequest(data: UserData): String {
        return fillTemplate(tokenIssuerWithdrawRequestTemplate, data)
    }

    private fun fillTemplate(template: Mustache, data: Any): String {
        val writer = StringWriter()
        template.execute(writer, data).flush()
        return writer.toString()
    }
}
