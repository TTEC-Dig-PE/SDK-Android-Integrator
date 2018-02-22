# SDK-Android-Integrator
An SDK Source Code example project written in Objective-C and Swift

For access to the SDK .aar files directly go here:
https://github.com/humanifydev/SDK-Android-Integrator/tree/master/app/libs

To Run: Open "MainApplication.java" Find the URL string (approx line 26) and the token string (approx line 27). Paste the auth token and URL provided by Humanify support here. 

Notes: Chats & callback will require an agent to answer. Please contact us for assistance setting up an agent account.

Release Notes: https://docs.google.com/document/d/1S6DOxTxL_LBT3BLvn-zZ7FWI6972H9_k2TiDheXyAUY

## Customizing the top navigation bar button items in chat

The top navigation bar buttons can now be customized. The behavior, colors, fonts, and icons can be changed. The top two navigation buttons are called the "Back" button (left) and the "End" button (right). 

The behavior of the buttons can be overridden using listeners. 

https://github.com/humanifydev/SDK-Android-Integrator/blob/master/app/src/main/java/com/humanify/expertconnect/sample/SampleActivity.java

* Back Button: See line 180
* End Button: See line 225

The fonts, colors, and text for the buttons are customized using strings. Override the following to change: 

https://github.com/humanifydev/SDK-Android-Integrator/blob/master/app/src/main/res/values/strings.xml

    <!-- Attribute to change End Chat button title -->
    <string name="expertconnect_end_chat">End Chat</string>

    <!-- ExpertConnect Custom Attributes -->
  
    <!-- Navigation back button customization(left button) attributes -->
    <!--<item name="expertconnect_chatBackButtonDrawable">@android:drawable/ic_menu_view</item>-->
    <!--<item name="expertconnect_chatBackButtonDrawableColor">@color/black</item>-->

    <!-- End Chat button customization(right button) attributes -->
    <!--<item name="expertconnect_chatEndButtonDrawable">@android:drawable/ic_menu_close_clear_cancel</item>-->
    <!--<item name="expertconnect_ChatEndButtonTextColor">@color/red</item>-->

    <!-- ExpertConnect Custom Attributes -->



## Getting Chat Skill Availability Details
The details of a chat or callback skill (such as estimated wait, chatReady, queueOpen) can be retrieved using the "getDetailsForExpertSkill" function. Example:

    getLoaderManager().restartLoader(0, null, new LoaderManager.LoaderCallbacks<ApiResult<SkillDetail>>() {
            @Override
            public Loader<ApiResult<SkillDetail>> onCreateLoader(int id, Bundle args) {
                return api.getDetailsForExpertSkill(skillName);
            }

            @Override
            public void onLoadFinished(Loader<ApiResult<SkillDetail>> loader, ApiResult<SkillDetail> data) {
                try {
                    SkillDetail skillDetail = data.get();
                } catch (ApiException e) {
                    Log.i("Error", e.getUserMessage(getResources()));
                }
            }

            @Override
            public void onLoaderReset(Loader<ApiResult<SkillDetail>> loader) {
            }
    });

Example can be found on line 408 of https://github.com/humanifydev/SDK-Android-Integrator/blob/master/app/src/main/java/com/humanify/expertconnect/sample/ChatActivity.java
  
The SkillDetail object contains the following fields:
* active - Whether this skill queue is active or not.
* chatCapacity - Maximum capacity of agents this skill can contain.
* chatReady - Number of agents who are ready to accept chats.
* description - Text description of this skill
* estWait - The estimated wait time to get connected (seconds)
* inQueue - Is this particular user in the queue already?
* queueOpen - Is the queue open or closed?
* skillName - Name of the skill
* voiceCapacity - Maximum capacity of agents who can take voice calls.
* voiceReady - Current number of agents ready to accept calls.

## Chat Persistence 
The chat persistence is implemented by loading chat history of the chat and populating the messages in chat screen. Example:
   
    getSupportLoaderManager().restartLoader(0, null, new LoaderManager.LoaderCallbacks<ApiResult<ConversationHistoryResponse>>() {
            @Override
            public Loader<ApiResult<ConversationHistoryResponse>> onCreateLoader(int id, Bundle args) {
                if (expertConnect.isChatActive()) {
                    String channelId = expertConnect.getChatChannel().getId();
                    String journeyId = expertConnect.getIdentityManager().getJourneyId();
                    if (journeyId != null && channelId != null) {
                        return api.getConversationHistoryDetail(journeyId, channelId);
                    }
                }
                return null;
            }

            public void onLoadFinished(Loader<ApiResult<ConversationHistoryResponse>> loader, ApiResult<ConversationHistoryResponse> data) {
                try {
                    ConversationHistoryResponse historyResponse = data.get();
                } catch (ApiException e) {
                    Log.i("Error", e.getUserMessage(getResources()));
                }
            }

            @Override
            public void onLoaderReset(Loader<ApiResult<ConversationHistoryResponse>> loader) {
            }
    });

## Disclaimer
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
