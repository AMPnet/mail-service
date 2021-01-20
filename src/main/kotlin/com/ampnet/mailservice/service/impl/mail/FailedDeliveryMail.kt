package com.ampnet.mailservice.service.impl.mail

import com.ampnet.mailservice.config.ApplicationProperties
import com.ampnet.mailservice.service.LinkResolverService
import com.ampnet.mailservice.service.TranslationService
import org.springframework.mail.javamail.JavaMailSender

class FailedDeliveryMail(
    mailSender: JavaMailSender,
    applicationProperties: ApplicationProperties,
    linkResolver: LinkResolverService,
    translationService: TranslationService
) : AbstractMail(mailSender, applicationProperties, linkResolver, translationService) {

    override val templateName = "failedDeliveryMessageTemplate"
    override val titleKey = "failedDeliveryTitle"

    fun setData(emails: List<String>) = apply { FailedDeliveryRecipients(emails.joinToString { ", " }) }
}

data class FailedDeliveryRecipients(val failedRecipients: String)
