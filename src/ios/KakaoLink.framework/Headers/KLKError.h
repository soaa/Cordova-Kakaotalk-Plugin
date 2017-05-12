/**
 * Copyright 2017 Kakao Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*!
 @header KLKError.h
 KakaoLink API를 호출할 때 발생하는 오류들을 정의합니다.
 */
#import <Foundation/Foundation.h>

/*!
 @constant KLKErrorDomain KakaoLink API에서 발생하는 NSError 객체의 도메인.
 */
extern NSString *const KLKErrorDomain;

/*!
 @abstract SDK 오류 코드 정의
 @constant KLKErrorUnknown 알 수 없는 오류
 @constant KLKErrorBadResponse 요청에 대한 응답에 기대하는 값이 없거나 문제가 있음
 @constant KLKErrorNetworkError 네트워크 오류
 @constant KLKErrorHTTP http 프로토콜 오류
 @constant KLKErrorBadParameter 파라미터 이상
 @constant KLKErrorMisConfigured 개발환경 설정 오류
 */
typedef NS_ENUM(NSInteger, KLKError) {
    KLKErrorUnknown = 1,
    KLKErrorBadResponse = 7,
    KLKErrorNetwork = 8,
    KLKErrorHTTP = 9,
    KLKErrorBadParameter = 11,
    KLKErrorMisConfigured = 12,
    
    KLKErrorUnsupportedTalkVersion = 201,
    KLKErrorExceedSizeLimit = 202,
};

/*!
 @abstract 서버 오류 코드 정의
 @constant KLKServerErrorUnknown 일반적인 서버 오류 응답. message를 확인해야 함
 @constant KLKServerErrorBadParameter 파라미터 이상
 @constant KLKServerErrorUnSupportedApi 지원되지 않은 API 호출
 @constant KLKServerErrorAccessDenied 해당 API에 대한 권한/퍼미션이 없는 경우
 @constant KLKServerErrorInternal 내부 서버 오류
 @constant KLKServerErrorApiLimitExceed API 호출 횟수가 제한을 초과
 @constant KLKServerErrorInvalidAppKey 등록되지 않은 앱키의 요청 또는 존재하지 않는 앱으로의 요청
 @constant KLKServerErrorExceedMaxUploadSize 이미지 업로드 사이즈 제한 초과
 @constant KLKServerErrorExecutionTimeOut 이미지 업로드시 타임아웃
 @constant KLKServerErrorExceedMaxUploadNumber 이미지 업로드시 허용된 업로드 파일 수가 넘을 경우
 @constant KLKServerErrorUnderMaintenance 서버 점검중
 */
typedef NS_ENUM(NSInteger, KLKServerError) {
    KLKServerErrorUnknown = -1,
    KLKServerErrorBadParameter = -2,
    KLKServerErrorUnSupportedApi = -3,
    KLKServerErrorAccessDenied = -5,
    KLKServerErrorInternal = -9,
    KLKServerErrorApiLimitExceed = -10,
    
    KLKServerErrorInvalidAppKey = -401,
    
    KLKServerErrorExceedMaxUploadSize = -602,
    KLKServerErrorExecutionTimeOut = -603,
    KLKServerErrorExceedMaxUploadNumber = -606,
    
    KLKServerErrorUnderMaintenance = -9798,
};
