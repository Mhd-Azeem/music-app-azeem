package com.wavelength.music.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wavelength.music.BuildConfig
import com.wavelength.music.data.remote.JamendoApiService
import com.wavelength.music.data.remote.jiosaavn.JioSaavnApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private fun loggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    // --- Jamendo -------------------------------------------------------------------------------

    @Provides
    @Singleton
    fun provideClientIdInterceptor(): Interceptor = Interceptor { chain ->
        val original = chain.request()
        val url = original.url.newBuilder()
            .addQueryParameter("client_id", BuildConfig.JAMENDO_CLIENT_ID)
            .addQueryParameter("format", "json")
            .build()
        chain.proceed(original.newBuilder().url(url).build())
    }

    @Provides
    @Singleton
    fun provideJamendoOkHttpClient(clientIdInterceptor: Interceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(clientIdInterceptor)
            .addInterceptor(loggingInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    @JamendoRetrofit
    fun provideJamendoRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.JAMENDO_BASE_URL.toHttpUrl())
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideJamendoApiService(@JamendoRetrofit retrofit: Retrofit): JamendoApiService =
        retrofit.create(JamendoApiService::class.java)

    // --- JioSaavn (unofficial, self-hosted deployment) ------------------------------------------

    @Provides
    @Singleton
    @JioSaavnRetrofit
    fun provideJioSaavnRetrofit(moshi: Moshi): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.JIOSAAVN_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideJioSaavnApiService(@JioSaavnRetrofit retrofit: Retrofit): JioSaavnApiService =
        retrofit.create(JioSaavnApiService::class.java)
}
