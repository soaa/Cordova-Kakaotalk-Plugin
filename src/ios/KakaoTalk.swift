import UIKit

@objc(KakaoTalk) class KakaoTalk: CDVPlugin {
    
    override func pluginInitialize() {
        NotificationCenter.default.addObserver(self, selector: #selector(KakaoTalk.applicationDidBecomeActive), name: Notification.Name.UIApplicationDidBecomeActive, object: nil)
    }
    
    func applicationDidBecomeActive(application: UIApplication) {
        KOSession.handleDidBecomeActive()
    }
    
    override func handleOpenURL(_ notification: Notification) {
        if let url = notification.object as? URL {
            if (KOSession.isKakaoAccountLoginCallback(url)) {
                NSLog(url.absoluteString)
                KOSession.handleOpen(url)
            }
        }
    }
    
    @objc(login:) func login(command: CDVInvokedUrlCommand) {
        let session:KOSession = KOSession.shared()
        
        if session.isOpen() {
            session.close()
        }
        
        session.open(completionHandler: { (error) in
            if(session.isOpen()) {
                NSLog("login success")
                KOSessionTask.meTask(completionHandler: { (response, error) in
                    if let user = response as? KOUser {
                        self.commandDelegate.send(
                            CDVPluginResult(status: CDVCommandStatus_OK,
                                            messageAs: [
                                                "id": user.id,
                                                "nickname": user.property(forKey: "nickname"),
                                                "profile_image": user.property(forKey: "profile_image"),
                                                "access_token": session.accessToken,
                                                "refresh_token": session.refreshToken
                                ]),
                            callbackId: command.callbackId
                            
                        )
                    } else {
                        NSLog("no response?")
                        self.commandDelegate.send(
                            CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "invalid response"),
                            callbackId: command.callbackId)
                    }
                })
            } else {
                // failed
                NSLog("login failed")
                
                self.commandDelegate.send(
                    CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: error?.localizedDescription),
                    callbackId: command.callbackId)
            }
            
        }, authParams: nil, authTypes: [NSNumber(value: KOAuthType.talk.rawValue)])
    }
    
    @objc(logout:) func logout(command: CDVInvokedUrlCommand) {
        let session:KOSession = KOSession.shared()
        
        session.logoutAndClose { (success, error) in
            NSLog("Logout result -> \(success)")
            
            let result = CDVPluginResult(status: CDVCommandStatus_OK)
            self.commandDelegate.send(result, callbackId: command.callbackId)
        }
    }
    
    @objc(canOpenTalkLink:) func canOpenTalkLink(command: CDVInvokedUrlCommand) {
        if(KLKTalkLinkCenter.shared().canOpenTalkLink()) {
            self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus_OK), callbackId: command.callbackId)
        } else {
            self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus_ERROR), callbackId: command.callbackId)
        }
    }
    
    @objc(share:) func share(command: CDVInvokedUrlCommand) {
        var kakaoArray = [KakaoTalkLinkObject]()
        
        if let options = command.arguments.first as? Dictionary<String, AnyObject> {
            if let text = options["text"] as? String {
                kakaoArray.append(KakaoTalkLinkObject.createLabel(text))
            }
            
            if let image = options["image"] as? Dictionary<String, AnyObject> {
                if let imageSrc = image["src"] as? String {
                    let w = max((image["width"] as? Int32) ?? 80, 80)
                    let h = max((image["height"] as? Int32) ?? 80, 80)
                    
                    kakaoArray.append(KakaoTalkLinkObject.createImage(imageSrc, width: w, height: h))
                }
            }
            
            if let weblink = options["weblink"] as? Dictionary<String, AnyObject> {
                if let weblinkUrl = weblink["url"] as? String, let weblinkText = weblink["text"] as? String {
                    kakaoArray.append(KakaoTalkLinkObject.createWebLink(weblinkText, url: weblinkUrl))
                }
            }
            
            if let applink = options["applink"] as? Dictionary<String, AnyObject> {
                if let applinkUrl = applink["url"] as? String, let applinkText = applink["text"] as? String {
                    kakaoArray.append(KakaoTalkLinkObject.createWebLink(applinkText, url: applinkUrl))
                    
                    let params = options["params"] as? Dictionary<String, AnyObject>
                    
                    let androidAppAction: KakaoTalkLinkAction = KakaoTalkLinkAction.createAppAction(KakaoTalkLinkActionOSPlatform.android, devicetype: KakaoTalkLinkActionDeviceType.phone, execparam: params)
                    
                    let iosAppAction: KakaoTalkLinkAction = KakaoTalkLinkAction.createAppAction(KakaoTalkLinkActionOSPlatform.IOS, devicetype: KakaoTalkLinkActionDeviceType.phone, execparam: params)
                    
                    let weblinkActionUsingPC: KakaoTalkLinkAction = KakaoTalkLinkAction.createWebAction(applinkUrl)
                    
                    let button: KakaoTalkLinkObject = KakaoTalkLinkObject.createAppButton(applinkText, actions: [androidAppAction, iosAppAction, weblinkActionUsingPC])
                    
                    kakaoArray.append(button)
                }
            }
        }
        
        var result: CDVPluginResult?
        
        if (kakaoArray.count > 0) {
            KOAppCall.openKakaoTalkAppLink(kakaoArray)
            
            result = CDVPluginResult(status: CDVCommandStatus_OK)
            
        } else {
            result = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "invalid parameter")
        }
        
        self.commandDelegate.send(result, callbackId: command.callbackId)
    }
    
    // Kakao Link v2
    @objc(link:) func link(command: CDVInvokedUrlCommand) {
        let template:KLKTemplate? = (command.arguments.first as? Dictionary<String, AnyObject>).flatMap { (options) -> KLKTemplate? in
            
            if let objectType = options["objectType"] as? String {
                switch objectType  {
                case "feed":
                    return KLKFeedTemplate { (templateBuilder: KLKFeedTemplateBuilder) in
                        if let content = options["content"] as? Dictionary<String, AnyObject> {
                            templateBuilder.content = KLKContentObject {(contentBuilder) in
                                contentBuilder.title = content["title"] as! String
                                contentBuilder.imageURL = URL(string: content["imageUrl"] as! String)!
                                contentBuilder.imageWidth = content["imageWidth"] as? NSNumber
                                contentBuilder.imageHeight = content["imageHeight"] as? NSNumber
                                contentBuilder.link = KLKLinkObject { (linkBuilder) in self.buildLink(linkBuilder: linkBuilder, link: content["link"] as! Dictionary<String, AnyObject>) }
                            }
                        }
                        templateBuilder.buttonTitle = options["buttonTitle"] as? String
                        
                        if let buttons = options["buttons"] as? Array<Dictionary<String, AnyObject>> {
                            buttons.forEach { (button) in
                                templateBuilder.addButton(KLKButtonObject { (buttonBuilder) in
                                    buttonBuilder.title = (button["title"] as? String) ?? ""
                                    buttonBuilder.link = KLKLinkObject { (linkBuilder) in self.buildLink(linkBuilder: linkBuilder, link: button["link"] as! Dictionary<String, AnyObject>) }
                                })
                            }
                        }
                    }
                    
                default:
                    return nil
                }
            }
            
            return nil
        }
        
        if (template != nil) {
            let talkLink = KLKTalkLinkCenter.shared()
            if(talkLink.canOpenTalkLink()) {
                NSLog("can open talk link")
            }
            talkLink.sendDefault(with: template!, success: { (warningMsg, argumentMsg) in
                NSLog("success")
                self.commandDelegate.send(
                    CDVPluginResult(status: CDVCommandStatus_OK),
                    callbackId: command.callbackId
                )
            }, failure: { (_ error: Error) in
                NSLog(String(describing: error))
                NSLog(error.localizedDescription)
                
                self.commandDelegate.send(
                    CDVPluginResult.init(status: CDVCommandStatus_ERROR, messageAs: error.localizedDescription),
                    callbackId: command.callbackId
                )
            })
        } else {
            self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "parameter error"), callbackId: command.callbackId)
        }
    }
    
    private func buildLink(linkBuilder: KLKLinkBuilder, link: Dictionary<String, AnyObject>) {
        linkBuilder.webURL = link["webUrl"].flatMap { url in URL(string: url as! String) }
        linkBuilder.mobileWebURL = link["mobileWebUrl"].flatMap { url in URL(string: url as! String) }
        linkBuilder.androidExecutionParams = link["androidExecParams"] as? String
        linkBuilder.iosExecutionParams = link["iosExecParams"] as? String
    }
}

