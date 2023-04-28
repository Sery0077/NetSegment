package sery.vlasenko.netsegment.data

import io.reactivex.rxjava3.core.Single
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.http.GET
import sery.vlasenko.netsegment.model.ApiResponse

interface IpService {

    @GET("/")
    fun getPublicIp(): Single<ResponseBody>

}