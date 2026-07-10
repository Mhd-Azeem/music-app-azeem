package com.wavelength.music.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.wavelength.music.BuildConfig
import com.wavelength.music.data.remote.jiosaavn.JioSaavnApiService
import com.wavelength.music.data.remote.lrclib.LrcLibApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Every model Moshi parses (JioSaavn/LrcLib DTOs, backup/QR export models) is annotated with
    // @JsonClass(generateAdapter = true), so a compile-time-generated adapter always exists —
    // no runtime-reflection Kotlin adapter fallback needed, which keeps this R8/minification-safe
    // without extra keep rules and avoids reflection's per-lookup overhead.
    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        // Transparently honors the backend's Cache-Control headers (max-age, stale-while-revalidate),
        // so responses survive process death and app restarts without any repository-level bookkeeping.
        val httpCache = Cache(File(context.cacheDir, "http_cache"), 25L * 1024 * 1024)
        return OkHttpClient.Builder()
            .cache(httpCache)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.JIOSAAVN_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideJioSaavnApiService(retrofit: Retrofit): JioSaavnApiService =
        retrofit.create(JioSaavnApiService::class.java)

    @Provides
    @Singleton
    fun provideLrcLibApiService(okHttpClient: OkHttpClient, moshi: Moshi): LrcLibApiService =
        Retrofit.Builder()
            .baseUrl("https://lrclib.net/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(LrcLibApiService::class.java)
}
