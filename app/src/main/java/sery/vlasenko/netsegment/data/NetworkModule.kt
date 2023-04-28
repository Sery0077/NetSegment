package sery.vlasenko.netsegment.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import sery.vlasenko.netsegment.BuildConfig
import java.util.concurrent.TimeUnit

object NetworkModule {
    val ipService: IpService = Retrofit.Builder()
        .baseUrl(BuildConfig.IP_URL)
        .addConverterFactory(
            provideMoshiConverterFactory()
        )
        .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
        .client(provideOkHttpClient())
        .build()
        .create(IpService::class.java)

    private fun provideMoshiConverterFactory(): MoshiConverterFactory = MoshiConverterFactory
        .create()
        .failOnUnknown()

    private fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .readTimeout(5, TimeUnit.SECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .build()

}