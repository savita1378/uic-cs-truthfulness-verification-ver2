package edu.uic.cs.wiki_verifier.wiki.local.search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.misc.LogHelper;

public class WikipediaSearcher
{
	private static final Logger LOGGER = LogHelper
			.getLogger(WikipediaSearcher.class);

	private WikipediaRawTextSearcher rawTextSearcher = new WikipediaRawTextSearcher();

	public Map<String, WikipediaPage> retrievePages(String title)
	{
		Map<String, String> pageRawTextsByTitle = rawTextSearcher
				.retrieveRawTextsByTitle(title);
		if (pageRawTextsByTitle == null)
		{
			return null;
		}

		Map<String, WikipediaPage> result = new HashMap<String, WikipediaPage>(
				pageRawTextsByTitle.size());
		for (Entry<String, String> rawTextByTitle : pageRawTextsByTitle
				.entrySet())
		{
			WikipediaPage rawPage = new WikipediaPage(rawTextByTitle.getKey(),
					rawTextByTitle.getValue());

			if (LOGGER.isDebugEnabled())
			{
				LOGGER.debug(rawPage + " has been parsed. ");
			}

			result.put(rawPage.getTitle(), rawPage);
		}

		return result;
	}

	private WikipediaPage retrieveCasePreservingTitle(String title)
	{
		Map<String, String> pageRawTextsByTitle = rawTextSearcher
				.retrieveRawTextsByTitle(title);
		if (pageRawTextsByTitle == null)
		{
			return null;
		}

		String rawText = pageRawTextsByTitle.get(title);

		WikipediaPage rawPage = null;
		if (rawText == null)
		{
			LOGGER.warn("No page be exact matched for title [" + title
					+ "]. Search for the non-redirect one instead. ");
			for (Entry<String, String> rawTextByTitle : pageRawTextsByTitle
					.entrySet())
			{
				WikipediaPage tempPage = new WikipediaPage(
						rawTextByTitle.getKey(), rawTextByTitle.getValue());
				if (!tempPage.isRedirectPage())
				{
					rawPage = tempPage;
					break;
				}
			}
		}
		else
		{
			rawPage = new WikipediaPage(title, rawText);
		}

		if (LOGGER.isDebugEnabled() && rawPage != null)
		{
			LOGGER.debug(rawPage + " has been parsed. ");
		}

		return rawPage;

	}

	public Set<WikipediaPage> retrieveNonRedirectedPage(String title)
	{
		if (LOGGER.isDebugEnabled())
		{
			LOGGER.debug(LogHelper.LOG_LAYER_ONE_BEGIN
					+ "retrieveNonRedirectedPage(" + title + ")");
		}

		Map<String, WikipediaPage> allRawPagesByTitle = retrievePages(title);
		if (allRawPagesByTitle == null)
		{
			return null;
		}

		Set<WikipediaPage> result = new HashSet<WikipediaPage>();
		for (WikipediaPage rawPage : allRawPagesByTitle.values())
		{
			if (!rawPage.isRedirectPage())
			{
				result.add(rawPage);
			}
			else
			{
				String redirectToTitle = rawPage.getRedirectToTitle();
				if (allRawPagesByTitle.containsKey(redirectToTitle))
				{
					if (LOGGER.isDebugEnabled())
					{
						LOGGER.debug("Raw page with [redirectToTitle="
								+ redirectToTitle
								+ "] has already been retrieved. ");
					}
					continue; // such title has been retrieved
				}

				if (LOGGER.isDebugEnabled())
				{
					LOGGER.debug("Try to retrieve Raw page for [redirectToTitle="
							+ redirectToTitle + "]. ");
				}

				int redirectToParagraphSymbolIndex = redirectToTitle
						.indexOf("#");
				if (redirectToParagraphSymbolIndex != -1)
				{
					String paragraphTitle = redirectToTitle
							.substring(redirectToParagraphSymbolIndex + 1);
					redirectToTitle = redirectToTitle.substring(0,
							redirectToParagraphSymbolIndex);

					if (LOGGER.isDebugEnabled())
					{
						LOGGER.debug("Redirecting to a paragraph ["
								+ paragraphTitle + "] within ["
								+ redirectToTitle + "]");
					}
				}

				// use case preserving title for redirect title
				WikipediaPage redirectToRawPage = retrieveCasePreservingTitle(redirectToTitle);
				Assert.notNull(redirectToRawPage,
						"There must be one page matched for [redirectToTitle="
								+ redirectToTitle + "] from " + rawPage);
				Assert.isTrue(!redirectToRawPage.isRedirectPage(),
						"Redirected page [" + redirectToRawPage
								+ "] redirected from " + rawPage
								+ " should not redirect to anthoer page. ");

				result.add(redirectToRawPage);
			}
		}

		if (result.size() != 1)
		{
			LOGGER.warn("Usually, there should be exactly one non-redirect raw page for title ["
					+ title + "], but there are " + result);
		}

		if (LOGGER.isDebugEnabled())
		{
			LOGGER.debug(LogHelper.LOG_LAYER_ONE_END
					+ "retrieveNonRedirectedPage(" + title + "). Result is "
					+ result);
		}

		return result;
	}
}
