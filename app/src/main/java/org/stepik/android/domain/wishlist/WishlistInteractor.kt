package org.stepik.android.domain.wishlist

import io.reactivex.Single
import org.stepic.droid.web.storage.model.StorageRecord
import org.stepik.android.cache.wishlist.mapper.WishlistEntityMapper
import org.stepik.android.cache.wishlist.model.WishlistEntity
import org.stepik.android.domain.base.DataSourceType
import org.stepik.android.domain.wishlist.model.WishlistWrapper
import org.stepik.android.domain.wishlist.repository.WishlistRepository
import javax.inject.Inject

class WishlistInteractor
@Inject
constructor(
    private val wishlistRepository: WishlistRepository,
    private val wishlistEntityMapper: WishlistEntityMapper
) {
    fun getWishlist(dataSourceType: DataSourceType = DataSourceType.REMOTE): Single<StorageRecord<WishlistWrapper>> =
        wishlistRepository.getWishlistRecord(dataSourceType)

    fun updateWishlistRecord(wishlistEntity: WishlistEntity): Single<WishlistEntity> =
        wishlistRepository
            .updateWishlistRecord(wishlistEntityMapper.mapToStorageRecord(wishlistEntity))
            .map(wishlistEntityMapper::mapToEntity)
}