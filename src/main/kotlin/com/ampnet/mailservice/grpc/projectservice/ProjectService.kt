package com.ampnet.mailservice.grpc.projectservice

import com.ampnet.projectservice.proto.OrganizationResponse
import com.ampnet.projectservice.proto.ProjectResponse
import com.ampnet.projectservice.proto.ProjectWithDataResponse
import java.util.UUID

interface ProjectService {
    fun getOrganization(uuid: UUID): OrganizationResponse
    fun getProject(uuid: UUID): ProjectResponse
    fun getProjectWithData(uuid: UUID): ProjectWithDataResponse
    fun getOrganizations(uuids: Iterable<UUID>): List<OrganizationResponse>
    fun getProjects(uuids: Iterable<UUID>): List<ProjectResponse>
}
