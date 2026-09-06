package com.wavelength.music.activation

import com.squareup.moshi.Moshi
import com.wavelength.music.BuildConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ActivationModule {
    @Binds
    @Singleton
    abstract fun bindActivationRepository(
        implementation: ActivationRepositoryImpl
    ): ActivationRepository

    companion object {
        @Provides
        @Singleton
        fun provideActivationApiService(
            okHttpClient: OkHttpClient,
            moshi: Moshi
        ): ActivationApiService = Retrofit.Builder()
            .baseUrl(BuildConfig.ACTIVATION_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ActivationApiService::class.java)
    }
}
