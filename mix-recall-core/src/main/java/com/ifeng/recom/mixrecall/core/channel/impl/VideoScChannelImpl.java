package com.ifeng.recom.mixrecall.core.channel.impl;

import com.ifeng.recom.mixrecall.common.constant.RecallConstant;
import com.ifeng.recom.mixrecall.common.constant.UserProfileEnum.TagPeriod;
import com.ifeng.recom.mixrecall.common.model.RecallResult;
import com.ifeng.recom.mixrecall.common.model.UserModel;
import com.ifeng.recom.mixrecall.common.model.request.LogicParams;
import com.ifeng.recom.mixrecall.common.model.request.MixRequestInfo;
import com.ifeng.recom.mixrecall.common.tool.ServiceLogUtil;
import com.ifeng.recom.mixrecall.common.util.UserProfileUtils;
import com.ifeng.recom.mixrecall.core.channel.excutor.csc.VideoScRecall;
import com.ifeng.recom.mixrecall.core.threadpool.ExecutorThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;


@Service
public class VideoScChannelImpl {
    private static final Logger logger = LoggerFactory.getLogger(VideoScChannelImpl.class);
    private static final int TIMEOUT = 300;
    private RecallResult.WeightAndPreloadPositionComparator weightAndPreloadPositionComparator = new RecallResult.WeightAndPreloadPositionComparator();


    public List<RecallResult> doRecall(MixRequestInfo mixRequestInfo) {
        long startTime = System.currentTimeMillis();
        String uid = mixRequestInfo.getUid();

        UserModel userModel = mixRequestInfo.getUserModel();
        LogicParams logicParams = mixRequestInfo.getLogicParams();
        List<RecallResult> result = callVideoSc(mixRequestInfo, logicParams, userModel);

        long cost = System.currentTimeMillis() - startTime;
        if (cost > 50) {
            ServiceLogUtil.debug("VideoScChannel {} cost:{}", uid, cost);
        }

        return result;
    }

    private List<RecallResult> callVideoSc(MixRequestInfo mixRequestInfo, LogicParams logicParams, UserModel userModel) {
        List<RecallResult> recallResults = new ArrayList<>();
        Map<String, Future<List<RecallResult>>> threadResultMap = new HashMap<>();
        Future<List<RecallResult>> videoScResult = null;
        try {
            videoScResult = ExecutorThreadPool.submitExecutorTask(new VideoScRecall(mixRequestInfo, UserProfileUtils.profileTagWeightFilter(userModel.getVideo_subcate(),RecallConstant.PROFILE_CUT_OFF_WEIGHT), TagPeriod.LONG, logicParams.videoScNum));
            if (videoScResult != null) {
                threadResultMap.put(TagPeriod.LONG.toString(), videoScResult);
            }
            Map<String, List<RecallResult>> result = ExecutorThreadPool.getExecutorResult(threadResultMap, TIMEOUT);
            recallResults = result.values().stream().collect(ArrayList::new, List::addAll, List::addAll);
            recallResults.sort(weightAndPreloadPositionComparator);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("{} VideoCChannelImpl ERROR:{}", mixRequestInfo.getUid(), e.toString());

        }

        return recallResults;
    }


}