/*
 * Copyright (C) 2014 Michell Bak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nmj.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.nmj.functions.FileSource;
import com.nmj.functions.NMJLib;
import com.nmj.nmjmanager.R;

import java.io.File;
import java.util.ArrayList;

import jcifs.smb.SmbFile;

public class DeleteFile extends IntentService {

	public DeleteFile() {
		super("DeleteFile");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String file = intent.getExtras().getString("filepath");

		if (file.startsWith("smb://")) {
			ArrayList<FileSource> filesources = NMJLib.getFileSources(NMJLib.TYPE_MOVIE, true);

			if (NMJLib.isWifiConnected(this)) {
				FileSource source = null;

				for (int j = 0; j < filesources.size(); j++)
					if (file.contains(filesources.get(j).getFilepath())) {
						source = filesources.get(j);
						break;
					}

				if (source == null)
					return;

				try {
					final SmbFile smbFile = new SmbFile(
							NMJLib.createSmbLoginString(
									source.getDomain(),
									source.getUser(),
									source.getPassword(),
									file,
									false
									));

					if (smbFile.exists())
						smbFile.delete();
					
					// Delete subtitles as well
					ArrayList<SmbFile> subs = new ArrayList<SmbFile>();

					String fileType = "";
					if (file.contains(".")) {
						fileType = file.substring(file.lastIndexOf("."));
					}

					int count = NMJLib.subtitleFormats.length;
					for (int i = 0; i < count; i++) {
						subs.add(new SmbFile(
								NMJLib.createSmbLoginString(
										source.getDomain(),
										source.getUser(),
										source.getPassword(),
										file.replace(fileType, NMJLib.subtitleFormats[i]),
										false
										)));
					}
					
					for (int i = 0; i < subs.size(); i++) {
						if (subs.get(i).exists())
							subs.get(i).delete();
					}
					
				} catch (Exception e) {
				}  // the file isn't available (either MalformedURLException or SmbException)
				showErrorMessage();
			}
		} else {
			boolean deleted;
			File f = new File(file);
			
			deleted = f.delete();
			if (!deleted)
				deleted = f.delete();
			
			if (!deleted)
				showErrorMessage();
			
			ArrayList<File> subs = new ArrayList<File>();

			String fileType = "";
			if (file.contains(".")) {
				fileType = file.substring(file.lastIndexOf("."));
			}

			int count = NMJLib.subtitleFormats.length;
			for (int i = 0; i < count; i++) {
				subs.add(new File(file.replace(fileType, NMJLib.subtitleFormats[i])));
			}
			
			for (int i = 0; i < subs.size(); i++) {
				if (subs.get(i).exists())
					subs.get(i).delete();
			}
		}
	}
	
	private void showErrorMessage() {
		Handler mHandler = new Handler(Looper.getMainLooper());
		mHandler.post(new Runnable() {            
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), R.string.failedDeleting, Toast.LENGTH_LONG).show();              
			}
		});
	}
}