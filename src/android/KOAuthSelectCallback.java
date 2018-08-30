package com.htj.plugin.kakao;

import com.kakao.auth.AuthType;

public interface KOAuthSelectCallback {

  void onSelect(AuthType authType);

  void onCancel();
}

