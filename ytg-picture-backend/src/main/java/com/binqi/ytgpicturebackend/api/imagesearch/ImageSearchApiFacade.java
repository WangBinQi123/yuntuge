package com.binqi.ytgpicturebackend.api.imagesearch;

import com.binqi.ytgpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.binqi.ytgpicturebackend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.binqi.ytgpicturebackend.api.imagesearch.sub.GetImageListApi;
import com.binqi.ytgpicturebackend.api.imagesearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@Slf4j
public class ImageSearchApiFacade {

    /**
     * 搜索图片
     *
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = null;
        try {
            imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }

    public static void main(String[] args) {
        // 测试以图搜图功能
        String imageUrl = "https://www.codefather.cn/logo.png";
        List<ImageSearchResult> resultList = searchImage(imageUrl);
        System.out.println("结果列表" + resultList);
    }
}
