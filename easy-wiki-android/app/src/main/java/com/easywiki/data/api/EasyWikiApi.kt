package com.easywiki.data.api

import com.easywiki.model.ApiResponse
import com.easywiki.model.AssignTaskRequest
import com.easywiki.model.AuthResponse
import com.easywiki.model.CreateGroupRequest
import com.easywiki.model.CreateTaskRequest
import com.easywiki.model.CreateWikiPageRequest
import com.easywiki.model.Group
import com.easywiki.model.LoginRequest
import com.easywiki.model.RegisterRequest
import com.easywiki.model.Task
import com.easywiki.model.TaskStatus
import com.easywiki.model.UpdateTaskRequest
import com.easywiki.model.UpdateWikiPageRequest
import com.easywiki.model.WikiPage
import com.easywiki.model.WikiTreeNode
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface EasyWikiApi {

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<Unit?>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthResponse>

    @GET("api/v1/health")
    suspend fun health(): ApiResponse<Map<String, String>>

    // Groups
    @GET("api/v1/groups")
    suspend fun listMyGroups(): ApiResponse<List<Group>>

    @POST("api/v1/groups")
    suspend fun createGroup(@Body request: CreateGroupRequest): ApiResponse<Group>

    @GET("api/v1/groups/{id}")
    suspend fun getGroup(@Path("id") id: Long): ApiResponse<Group>

    @POST("api/v1/groups/join/{token}")
    suspend fun joinByInvite(@Path("token") token: String): ApiResponse<Group>

    // Wiki
    @GET("api/v1/groups/{groupId}/wiki/tree")
    suspend fun getWikiTree(@Path("groupId") groupId: Long): ApiResponse<List<WikiTreeNode>>

    @GET("api/v1/groups/{groupId}/wiki/pages/{pageId}")
    suspend fun getWikiPage(
        @Path("groupId") groupId: Long,
        @Path("pageId") pageId: Long
    ): ApiResponse<WikiPage>

    @POST("api/v1/groups/{groupId}/wiki/pages")
    suspend fun createWikiPage(
        @Path("groupId") groupId: Long,
        @Body request: CreateWikiPageRequest
    ): ApiResponse<WikiPage>

    @PUT("api/v1/groups/{groupId}/wiki/pages/{pageId}")
    suspend fun updateWikiPage(
        @Path("groupId") groupId: Long,
        @Path("pageId") pageId: Long,
        @Body request: UpdateWikiPageRequest
    ): ApiResponse<WikiPage>

    // Tasks
    @GET("api/v1/groups/{groupId}/tasks")
    suspend fun listTasks(
        @Path("groupId") groupId: Long,
        @Query("status") status: TaskStatus? = null
    ): ApiResponse<List<Task>>

    @GET("api/v1/groups/{groupId}/tasks/{taskId}")
    suspend fun getTask(
        @Path("groupId") groupId: Long,
        @Path("taskId") taskId: Long
    ): ApiResponse<Task>

    @POST("api/v1/groups/{groupId}/tasks")
    suspend fun createTask(
        @Path("groupId") groupId: Long,
        @Body request: CreateTaskRequest
    ): ApiResponse<Task>

    @PUT("api/v1/groups/{groupId}/tasks/{taskId}")
    suspend fun updateTask(
        @Path("groupId") groupId: Long,
        @Path("taskId") taskId: Long,
        @Body request: UpdateTaskRequest
    ): ApiResponse<Task>

    @POST("api/v1/groups/{groupId}/tasks/{taskId}/assign")
    suspend fun assignTask(
        @Path("groupId") groupId: Long,
        @Path("taskId") taskId: Long,
        @Body request: AssignTaskRequest
    ): ApiResponse<Task>

    @POST("api/v1/groups/{groupId}/tasks/{taskId}/accept")
    suspend fun acceptTask(
        @Path("groupId") groupId: Long,
        @Path("taskId") taskId: Long
    ): ApiResponse<Task>

    @POST("api/v1/groups/{groupId}/tasks/{taskId}/reject")
    suspend fun rejectTask(
        @Path("groupId") groupId: Long,
        @Path("taskId") taskId: Long
    ): ApiResponse<Task>

    @POST("api/v1/groups/{groupId}/tasks/{taskId}/claim")
    suspend fun claimTask(
        @Path("groupId") groupId: Long,
        @Path("taskId") taskId: Long
    ): ApiResponse<Task>
}
