# slack-notifications

This service enables sending messages into the HMRC Digital workspace on Slack.

The service exposes 2 endpoints:

```
POST    /notification    # uses legacy incoming webhooks
POST    /v2/notification # uses PlatOps Bot (recommended)
```

Both endpoints require a `channelLookup` in order to identify the correct channel for the message to be posted to.

## ChannelLookup

Can be one of:

`github-repository` - will attempt to find the channel for the team that owns the repository.

```json
{
    "by" : "github-repository",
    "repositoryName" : "name-of-a-repo"
}
```

`github-team` - will attempt to find the channel for the GitHub team.

```json
{
    "by" : "github-team",
    "teamName" : "name-of-a-github-team"
}
```

`slack-channel` - will attempt to send the message to all channels in the array.

```json
{
    "by" : "slack-channel",
    "slackChannels" : [
        "channel1",
        "channel2"
    ]
}
```

`teams-of-github-user` - will attempt to send the message to the channel of the team the user belongs to.

```json
{
    "by" : "teams-of-github-user",
    "githubUsername" : "a-github-username"
}
```

`teams-of-ldap-user` - will attempt to send the message to the channel of the team the user belongs to.

```json
{
    "by" : "teams-of-ldap-user",
    "ldapUsername" : "an-ldap-username"
}
```

## Setup and example usage of `POST    /notification`

### Auth
This endpoint uses Basic Auth for access control. If you want to use it please contact team PlatOps.

### Adding new users able to send Slack Notifications

The list of users that are able to use the service is predefined by an array in the config:

```
auth {
    authorizedServices = [
        {
            name = test
            password = "dGVzdA=="
            displayName = "My Bot"
            userEmoji = ":male-mechanic:"
        }
    ]
}
```
Where:
 * `name` is the username
 * `password` is a base64 encoded password for the user
 * Optional: `displayName` is a friendly name to use for sending messages as. If not set, will use `name` instead
 * Optional: `userEmoji` is the icon to use for when sending messages for this user
 
### Base64 encoded password

Please note that omitting `-n` will result in a new line character as a part of the base64 encoded string. Where this is unintentional, the password from the basic auth header will not match resulting in a 401 auth failed response.

```
echo -n "password" | base64
```
### In HMRC

If you would like to add a new user that is able to send Slack notifications then you will need to submit a PR to the following repos:

  1. https://github.com/hmrc/app-config-platapps-labs/blob/master/slack-notifications.yaml#L73-L74
  1. https://github.com/hmrc/app-config-platapps-live/blob/master/slack-notifications.yaml#L75-L76

> Remember to base64 and then encrypt the passwords (as described in the configs above)

Once we receive the PR we will review, before redeploying the app.

**N.B.** This only applies to users within the HMRC organisation on github

### Example request

Sends Slack messages to all teams contributing to a repo as shown in The Catalogue.
If a repository defines owners explicitly in the 'repository.yaml' file, Slack message will be sent only to those teams (relevant mostly for shared repos like app-config-*).

Here `attachments` should be structured as defined in the [Slack documentation](https://api.slack.com/reference/messaging/attachments)

Note: `channelLookup` can be replaced with any of the ones mentioned above, depending on the use case.
```
POST /slack-notifications/notification

body:

{
    "channelLookup" : {
        "by" : "github-repository",
        "repositoryName" : "name-of-a-repo"
    },
    "messageDetails" : {
        "text" : "message to be posted",
        "attachments" : [ // optional
            { "text" : "some-attachment" }
        ]    
    }
}
```

### Example curl request

Assuming basic auth credentials for user: foo, pass: bar, i.e.: user:bar (Base64 encoded) = Zm9vOmJhcg==

```
curl -X POST -H 'Content-type: application/json' -H 'Authorization: Basic Zm9vOmJhcg==' \
    --data '{"channelLookup" : { "by" : "github-repository", "repositoryName" : "foo" }, "messageDetails" : { "text" : "Testing if slack-notifications work" } }' \
    localhost:8866/slack-notifications/notification
```

## Setup and example usage of `POST    /v2/notification`

### Auth

This endpoint uses `internal-auth` for access control. If you want to use it then you will need to fork [internal-auth-config](https://github.com/hmrc/internal-auth-config) and raise a PR adding your service to the list of grantees for the `slack-notifications` resource type.

### Example request

Sends a Slack message to the channel specified

Here `text` is the text that will be displayed in the desktop notification, think of this like alt text for an image. It will not be displayed in the main body of the message.

`blocks` can be designed using the [Slack Block Kit Builder](https://app.slack.com/block-kit-builder)

```
POST    /slack-notifications/v2/notification

headers: Authorization: <internal-auth-token>

body:

{
    "channelLookup": {
        "by": "slack-channel",
        "slackChannels": [
            "channel1"
        ]
    },
    "displayName": "Example", # username associated with the message
    "emoji": ":robot_face:",  # acts as the profile picture
    "text": "Example message",
    "blocks": [
        ...
    ]
}
```

## Response

Both endpoints share the same response structure, including error codes.

Response will typically have 200 status code and the following details:

```

{
    "successfullySentTo" : [
        "channel1",
        "channel2"
    ],
    "errors" : [
        {   
            "code" : "error_code_1",
            "message" : "Details of a problem"
        },
        {
            "code" : "error_code_2",
            "message" : "Details of another problem"
        }
    ],
    "exclusions" : [
        {
            "code" : "exclusion_code",
            "message" : "Details of why slack message was not sent"
        }
    ]
}

# error/exclusion codes are stable, messages may change

```

### Possible error codes are:

|Error Code                              | Meaning                                                                |
|----------------------------------------|------------------------------------------------------------------------|
|repository_not_found                    | A repository could not be found                                        |
|teams_not_found_for_repository          | The teams responsible for a repository could not be found              |
|teams_not_found_for_github_username     | No teams could be found for the given github username                  |
|slack_channel_not_found                 | The slack channel was not found                                        |  
|slack_error                             | A generic error wrapping an exception coming directly from Slack       |

### Possible exclusion codes are:

|Exclusion Code                          | Meaning
|----------------------------------------|------------------------------------------------------------------------|
|not_a_real_team                         | Team is not a real MDTP team with human members                        |
|not_a_real_github_user                  | Github user is not a real person, e.g. CI user                         |

### Allow listed domains

Any URL can be checked against the allow listed domains stored in LinkUtils.scala.

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
