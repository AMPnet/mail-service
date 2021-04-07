package com.ampnet.mailservice.service.impl.mail

import com.ampnet.mailservice.config.ApplicationProperties
import com.ampnet.mailservice.enums.MailType
import com.ampnet.mailservice.service.CmsService
import com.ampnet.mailservice.service.LinkResolverService
import org.springframework.mail.javamail.JavaMailSender

class FailedDeliveryMail(
    linkResolver: LinkResolverService,
    mailSender: JavaMailSender,
    applicationProperties: ApplicationProperties,
    cmsService: CmsService
) : AbstractMail(linkResolver, mailSender, applicationProperties, cmsService) {

    override val mailType = MailType.FAILED_DELIVERY_MAIL

    fun setTemplateData(emails: List<String>) = apply {
        templateData = FailedDeliveryRecipients(emails.joinToString { ", " })
    }
}

data class FailedDeliveryRecipients(val failedRecipients: String)
